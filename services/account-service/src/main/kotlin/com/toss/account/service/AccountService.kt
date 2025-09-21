package com.toss.account.service

import com.toss.account.dto.*
import com.toss.account.entity.*
import com.toss.account.repository.AccountRepository
import com.toss.account.repository.AccountTransactionRepository
import com.toss.shared.exception.ErrorCodes
import com.toss.shared.exception.TossException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository,
    private val accountTransactionRepository: AccountTransactionRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    fun createAccount(userId: Long, request: CreateAccountRequest): AccountResponse {
        // Check if user already has an account of this type
        val existingAccounts = accountRepository.findByUserIdAndAccountTypeAndStatus(
            userId, request.accountType, AccountStatus.ACTIVE
        )
        
        if (existingAccounts.isNotEmpty()) {
            throw TossException(ErrorCodes.ACCOUNT_ALREADY_EXISTS, "Account of this type already exists")
        }
        
        // Generate unique account number
        val accountNumber = generateAccountNumber()
        
        // Create account
        val account = Account(
            accountNumber = accountNumber,
            userId = userId,
            accountName = request.accountName,
            accountType = request.accountType,
            balance = request.initialBalance,
            availableBalance = request.initialBalance
        )
        
        val savedAccount = accountRepository.save(account)
        
        // Create initial transaction if there's an initial balance
        if (request.initialBalance > BigDecimal.ZERO) {
            createTransaction(
                savedAccount.id,
                "INIT_${System.currentTimeMillis()}",
                TransactionType.DEPOSIT,
                request.initialBalance,
                request.initialBalance,
                "Initial deposit"
            )
        }
        
        // Publish account created event
        publishAccountEvent("ACCOUNT_CREATED", savedAccount)
        
        return savedAccount.toAccountResponse()
    }
    
    @Transactional(readOnly = true)
    fun getAccountsByUserId(userId: Long): List<AccountResponse> {
        val accounts = accountRepository.findByUserIdAndStatus(userId, AccountStatus.ACTIVE)
        return accounts.map { it.toAccountResponse() }
    }
    
    @Transactional(readOnly = true)
    fun getAccountSummary(userId: Long): AccountSummaryResponse {
        val accounts = accountRepository.findByUserIdAndStatus(userId, AccountStatus.ACTIVE)
        val totalBalance = accounts.sumOf { it.balance }
        val availableBalance = accounts.sumOf { it.availableBalance }
        
        return AccountSummaryResponse(
            totalAccounts = accounts.size,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            accounts = accounts.map { it.toAccountResponse() }
        )
    }
    
    @Transactional(readOnly = true)
    fun getAccountByNumber(accountNumber: String): AccountResponse {
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: throw TossException(ErrorCodes.ACCOUNT_NOT_FOUND, "Account not found")
        
        return account.toAccountResponse()
    }
    
    @Transactional(readOnly = true)
    fun getAccountTransactions(
        accountId: Long, 
        page: Int = 0, 
        size: Int = 20
    ): Page<AccountTransactionResponse> {
        val pageable: Pageable = PageRequest.of(page, size)
        val transactions = accountTransactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
        return transactions.map { it.toAccountTransactionResponse() }
    }
    
    fun updateAccount(accountId: Long, userId: Long, request: UpdateAccountRequest): AccountResponse {
        val account = accountRepository.findById(accountId)
            .orElseThrow { TossException(ErrorCodes.ACCOUNT_NOT_FOUND, "Account not found") }
        
        if (account.userId != userId) {
            throw TossException(ErrorCodes.AUTH_ACCESS_DENIED, "Access denied")
        }
        
        val updatedAccount = account.copy(
            accountName = request.accountName ?: account.accountName,
            dailyLimit = request.dailyLimit ?: account.dailyLimit,
            monthlyLimit = request.monthlyLimit ?: account.monthlyLimit
        )
        
        val savedAccount = accountRepository.save(updatedAccount)
        return savedAccount.toAccountResponse()
    }
    
    fun transfer(request: TransferRequest, userId: Long): TransferResponse {
        // Check idempotency
        val idempotencyKey = "transfer:${request.idempotencyKey}"
        if (redisTemplate.hasKey(idempotencyKey)) {
            val cachedResult = redisTemplate.opsForValue().get(idempotencyKey)
            // Return cached result (would need to deserialize properly in real implementation)
            throw TossException(ErrorCodes.TRANSACTION_DUPLICATE, "Duplicate transaction")
        }
        
        // Lock both accounts for transfer
        val fromAccount = accountRepository.findByAccountNumberWithLock(request.fromAccountNumber, AccountStatus.ACTIVE)
            ?: throw TossException(ErrorCodes.ACCOUNT_NOT_FOUND, "From account not found")
        
        val toAccount = accountRepository.findByAccountNumberWithLock(request.toAccountNumber, AccountStatus.ACTIVE)
            ?: throw TossException(ErrorCodes.ACCOUNT_NOT_FOUND, "To account not found")
        
        // Validate ownership
        if (fromAccount.userId != userId) {
            throw TossException(ErrorCodes.AUTH_ACCESS_DENIED, "Access denied to from account")
        }
        
        // Validate transfer
        validateTransfer(fromAccount, request.amount)
        
        // Generate transaction ID
        val transactionId = "TXN_${System.currentTimeMillis()}_${userId}"
        
        try {
            // Update balances
            val fromNewBalance = fromAccount.balance.subtract(request.amount)
            val toNewBalance = toAccount.balance.add(request.amount)
            
            val updatedFromAccount = fromAccount.copy(
                balance = fromNewBalance,
                availableBalance = fromNewBalance,
                dailyUsedAmount = fromAccount.dailyUsedAmount.add(request.amount),
                monthlyUsedAmount = fromAccount.monthlyUsedAmount.add(request.amount),
                lastUsedAt = LocalDateTime.now()
            )
            
            val updatedToAccount = toAccount.copy(
                balance = toNewBalance,
                availableBalance = toNewBalance,
                lastUsedAt = LocalDateTime.now()
            )
            
            accountRepository.save(updatedFromAccount)
            accountRepository.save(updatedToAccount)
            
            // Create transactions
            createTransaction(
                fromAccount.id,
                transactionId,
                TransactionType.TRANSFER_OUT,
                request.amount.negate(),
                fromNewBalance,
                request.description ?: "Transfer to ${toAccount.accountNumber}"
            )
            
            createTransaction(
                toAccount.id,
                transactionId,
                TransactionType.TRANSFER_IN,
                request.amount,
                toNewBalance,
                request.description ?: "Transfer from ${fromAccount.accountNumber}"
            )
            
            // Cache result for idempotency
            redisTemplate.opsForValue().set(idempotencyKey, transactionId, 24, TimeUnit.HOURS)
            
            // Publish transfer event
            publishTransferEvent(transactionId, fromAccount.accountNumber, toAccount.accountNumber, request.amount)
            
            return TransferResponse(
                transactionId = transactionId,
                fromAccountNumber = fromAccount.accountNumber,
                toAccountNumber = toAccount.accountNumber,
                amount = request.amount,
                status = TransactionStatus.COMPLETED,
                processedAt = LocalDateTime.now()
            )
            
        } catch (e: Exception) {
            throw TossException(ErrorCodes.TRANSACTION_NOT_FOUND, "Transfer failed: ${e.message}")
        }
    }
    
    private fun validateTransfer(account: Account, amount: BigDecimal) {
        if (account.status != AccountStatus.ACTIVE) {
            throw TossException(ErrorCodes.ACCOUNT_INACTIVE, "Account is not active")
        }
        
        if (account.availableBalance < amount) {
            throw TossException(ErrorCodes.ACCOUNT_INSUFFICIENT_BALANCE, "Insufficient balance")
        }
        
        // Check daily limit
        val todayUsed = if (isSameDay(account.lastUsedAt, LocalDateTime.now())) {
            account.dailyUsedAmount
        } else {
            BigDecimal.ZERO
        }
        
        if (todayUsed.add(amount) > account.dailyLimit) {
            throw TossException(ErrorCodes.TRANSACTION_LIMIT_EXCEEDED, "Daily limit exceeded")
        }
        
        // Check monthly limit
        val monthlyUsed = if (isSameMonth(account.lastUsedAt, LocalDateTime.now())) {
            account.monthlyUsedAmount
        } else {
            BigDecimal.ZERO
        }
        
        if (monthlyUsed.add(amount) > account.monthlyLimit) {
            throw TossException(ErrorCodes.TRANSACTION_LIMIT_EXCEEDED, "Monthly limit exceeded")
        }
    }
    
    private fun createTransaction(
        accountId: Long,
        transactionId: String,
        type: TransactionType,
        amount: BigDecimal,
        balanceAfter: BigDecimal,
        description: String?
    ): AccountTransaction {
        val transaction = AccountTransaction(
            accountId = accountId,
            transactionId = transactionId,
            transactionType = type,
            amount = amount,
            balanceAfter = balanceAfter,
            description = description
        )
        
        return accountTransactionRepository.save(transaction)
    }
    
    private fun generateAccountNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "TOSS${timestamp}${random}"
    }
    
    private fun isSameDay(date1: LocalDateTime?, date2: LocalDateTime): Boolean {
        return date1?.toLocalDate() == date2.toLocalDate()
    }
    
    private fun isSameMonth(date1: LocalDateTime?, date2: LocalDateTime): Boolean {
        return date1?.toLocalDate()?.withDayOfMonth(1) == date2.toLocalDate().withDayOfMonth(1)
    }
    
    private fun publishAccountEvent(eventType: String, account: Account) {
        val event = mapOf(
            "eventType" to eventType,
            "accountId" to account.id,
            "userId" to account.userId,
            "accountNumber" to account.accountNumber,
            "timestamp" to LocalDateTime.now()
        )
        kafkaTemplate.send("account-events", event)
    }
    
    private fun publishTransferEvent(transactionId: String, fromAccount: String, toAccount: String, amount: BigDecimal) {
        val event = mapOf(
            "eventType" to "TRANSFER_COMPLETED",
            "transactionId" to transactionId,
            "fromAccount" to fromAccount,
            "toAccount" to toAccount,
            "amount" to amount,
            "timestamp" to LocalDateTime.now()
        )
        kafkaTemplate.send("transfer-events", event)
    }
}
