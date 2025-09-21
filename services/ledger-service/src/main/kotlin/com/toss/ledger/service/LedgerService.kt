package com.toss.ledger.service

import com.toss.ledger.dto.*
import com.toss.ledger.entity.*
import com.toss.ledger.repository.AccountBalanceRepository
import com.toss.ledger.repository.LedgerEntryRepository
import com.toss.shared.exception.ErrorCodes
import com.toss.shared.exception.TossException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class LedgerService(
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    fun createLedgerEntry(request: CreateLedgerEntryRequest): LedgerEntryResponse {
        // Get or create account balance
        val accountBalance = accountBalanceRepository.findByAccountIdWithLock(request.accountId)
            ?: createAccountBalance(request.accountId, request.accountNumber)
        
        // Calculate new balance
        val newBalance = when (request.entryType) {
            LedgerEntryType.DEBIT -> accountBalance.balance.subtract(request.amount)
            LedgerEntryType.CREDIT -> accountBalance.balance.add(request.amount)
        }
        
        // Validate balance for debit
        if (request.entryType == LedgerEntryType.DEBIT && newBalance < BigDecimal.ZERO) {
            throw TossException(ErrorCodes.LEDGER_BALANCE_MISMATCH, "Insufficient balance for debit")
        }
        
        // Create ledger entry
        val ledgerEntry = LedgerEntry(
            transactionId = request.transactionId,
            accountId = request.accountId,
            accountNumber = request.accountNumber,
            entryType = request.entryType,
            amount = request.amount,
            balanceAfter = newBalance,
            description = request.description,
            referenceId = request.referenceId
        )
        
        val savedEntry = ledgerEntryRepository.save(ledgerEntry)
        
        // Update account balance
        val updatedBalance = accountBalance.copy(
            balance = newBalance,
            availableBalance = newBalance.subtract(accountBalance.frozenAmount),
            lastTransactionId = request.transactionId,
            lastTransactionAt = LocalDateTime.now()
        )
        accountBalanceRepository.save(updatedBalance)
        
        // Publish ledger event
        publishLedgerEvent("LEDGER_ENTRY_CREATED", savedEntry)
        
        return savedEntry.toLedgerEntryResponse()
    }
    
    @Transactional(readOnly = true)
    fun getLedgerEntries(
        accountId: Long,
        page: Int = 0,
        size: Int = 20,
        entryType: LedgerEntryType? = null,
        status: LedgerEntryStatus? = null
    ): Page<LedgerEntryResponse> {
        val pageable: Pageable = PageRequest.of(page, size)
        
        val entries = when {
            entryType != null -> ledgerEntryRepository.findByAccountIdAndEntryTypeOrderByCreatedAtDesc(accountId, entryType, pageable)
            status != null -> ledgerEntryRepository.findByAccountIdAndStatusOrderByCreatedAtDesc(accountId, status, pageable)
            else -> ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
        }
        
        return entries.map { it.toLedgerEntryResponse() }
    }
    
    @Transactional(readOnly = true)
    fun getAccountBalance(accountId: Long): AccountBalanceResponse {
        val balance = accountBalanceRepository.findByAccountId(accountId)
            ?: throw TossException(ErrorCodes.ACCOUNT_NOT_FOUND, "Account balance not found")
        
        return balance.toAccountBalanceResponse()
    }
    
    @Transactional(readOnly = true)
    fun getLedgerSummary(accountId: Long): LedgerSummaryResponse {
        val totalDebits = ledgerEntryRepository.getTotalAmountByAccountAndTypeAndStatus(
            accountId, LedgerEntryType.DEBIT, LedgerEntryStatus.ACTIVE
        ) ?: BigDecimal.ZERO
        
        val totalCredits = ledgerEntryRepository.getTotalAmountByAccountAndTypeAndStatus(
            accountId, LedgerEntryType.CREDIT, LedgerEntryStatus.ACTIVE
        ) ?: BigDecimal.ZERO
        
        val totalEntries = ledgerEntryRepository.countEntriesByAccountAndTypeAndStatusSince(
            accountId, LedgerEntryType.DEBIT, LedgerEntryStatus.ACTIVE, LocalDateTime.of(2020, 1, 1, 0, 0)
        ) + ledgerEntryRepository.countEntriesByAccountAndTypeAndStatusSince(
            accountId, LedgerEntryType.CREDIT, LedgerEntryStatus.ACTIVE, LocalDateTime.of(2020, 1, 1, 0, 0)
        )
        
        val activeEntries = ledgerEntryRepository.countEntriesByAccountAndTypeAndStatusSince(
            accountId, LedgerEntryType.DEBIT, LedgerEntryStatus.ACTIVE, LocalDateTime.of(2020, 1, 1, 0, 0)
        ) + ledgerEntryRepository.countEntriesByAccountAndTypeAndStatusSince(
            accountId, LedgerEntryType.CREDIT, LedgerEntryStatus.ACTIVE, LocalDateTime.of(2020, 1, 1, 0, 0)
        )
        
        val reversedEntries = ledgerEntryRepository.countEntriesByAccountAndTypeAndStatusSince(
            accountId, LedgerEntryType.DEBIT, LedgerEntryStatus.REVERSED, LocalDateTime.of(2020, 1, 1, 0, 0)
        ) + ledgerEntryRepository.countEntriesByAccountAndTypeAndStatusSince(
            accountId, LedgerEntryType.CREDIT, LedgerEntryStatus.REVERSED, LocalDateTime.of(2020, 1, 1, 0, 0)
        )
        
        return LedgerSummaryResponse(
            totalEntries = totalEntries,
            totalDebits = totalDebits,
            totalCredits = totalCredits,
            netBalance = totalCredits.subtract(totalDebits),
            activeEntries = activeEntries,
            reversedEntries = reversedEntries
        )
    }
    
    fun reverseLedgerEntry(entryId: Long, request: ReverseLedgerEntryRequest): LedgerEntryResponse {
        val entry = ledgerEntryRepository.findById(entryId)
            .orElseThrow { TossException(ErrorCodes.TRANSACTION_NOT_FOUND, "Ledger entry not found") }
        
        if (entry.status != LedgerEntryStatus.ACTIVE) {
            throw TossException(ErrorCodes.TRANSACTION_NOT_FOUND, "Entry cannot be reversed")
        }
        
        // Create reversal entry
        val reversalEntry = entry.copy(
            id = 0, // New entry
            entryType = if (entry.entryType == LedgerEntryType.DEBIT) LedgerEntryType.CREDIT else LedgerEntryType.DEBIT,
            amount = entry.amount.negate(),
            balanceAfter = entry.balanceAfter.add(entry.amount),
            description = "Reversal: ${entry.description}",
            referenceId = entry.id.toString(),
            status = LedgerEntryStatus.ACTIVE
        )
        
        val savedReversal = ledgerEntryRepository.save(reversalEntry)
        
        // Mark original entry as reversed
        val reversedEntry = entry.copy(
            status = LedgerEntryStatus.REVERSED,
            reversedAt = LocalDateTime.now(),
            reversedBy = "system",
            reversalReason = request.reason ?: "Manual reversal"
        )
        ledgerEntryRepository.save(reversedEntry)
        
        // Update account balance
        val accountBalance = accountBalanceRepository.findByAccountIdWithLock(entry.accountId)
        if (accountBalance != null) {
            val newBalance = accountBalance.balance.add(entry.amount)
            val updatedBalance = accountBalance.copy(
                balance = newBalance,
                availableBalance = newBalance.subtract(accountBalance.frozenAmount),
                lastTransactionId = savedReversal.transactionId,
                lastTransactionAt = LocalDateTime.now()
            )
            accountBalanceRepository.save(updatedBalance)
        }
        
        // Publish reversal event
        publishLedgerEvent("LEDGER_ENTRY_REVERSED", savedReversal)
        
        return savedReversal.toLedgerEntryResponse()
    }
    
    fun adjustBalance(request: BalanceAdjustmentRequest): AccountBalanceResponse {
        val accountBalance = accountBalanceRepository.findByAccountIdWithLock(request.accountId)
            ?: createAccountBalance(request.accountId, request.accountNumber)
        
        val newBalance = accountBalance.balance.add(request.amount)
        
        if (newBalance < BigDecimal.ZERO) {
            throw TossException(ErrorCodes.LEDGER_BALANCE_MISMATCH, "Balance cannot be negative")
        }
        
        val updatedBalance = accountBalance.copy(
            balance = newBalance,
            availableBalance = newBalance.subtract(accountBalance.frozenAmount),
            lastTransactionId = request.transactionId,
            lastTransactionAt = LocalDateTime.now()
        )
        
        val savedBalance = accountBalanceRepository.save(updatedBalance)
        
        // Create adjustment entry
        val entryType = if (request.amount > BigDecimal.ZERO) LedgerEntryType.CREDIT else LedgerEntryType.DEBIT
        val adjustmentEntry = LedgerEntry(
            transactionId = request.transactionId,
            accountId = request.accountId,
            accountNumber = request.accountNumber,
            entryType = entryType,
            amount = request.amount.abs(),
            balanceAfter = newBalance,
            description = request.description ?: "Balance adjustment",
            referenceId = "ADJUSTMENT"
        )
        ledgerEntryRepository.save(adjustmentEntry)
        
        // Publish adjustment event
        publishLedgerEvent("BALANCE_ADJUSTED", savedBalance)
        
        return savedBalance.toAccountBalanceResponse()
    }
    
    @Transactional(readOnly = true)
    fun reconcileBalance(accountId: Long): BalanceReconciliationResponse {
        val accountBalance = accountBalanceRepository.findByAccountId(accountId)
            ?: throw TossException(ErrorCodes.ACCOUNT_NOT_FOUND, "Account balance not found")
        
        val totalDebits = ledgerEntryRepository.getTotalAmountByAccountAndTypeAndStatus(
            accountId, LedgerEntryType.DEBIT, LedgerEntryStatus.ACTIVE
        ) ?: BigDecimal.ZERO
        
        val totalCredits = ledgerEntryRepository.getTotalAmountByAccountAndTypeAndStatus(
            accountId, LedgerEntryType.CREDIT, LedgerEntryStatus.ACTIVE
        ) ?: BigDecimal.ZERO
        
        val ledgerBalance = totalCredits.subtract(totalDebits)
        val difference = accountBalance.balance.subtract(ledgerBalance)
        val isReconciled = difference.abs() < BigDecimal("0.01") // Allow for rounding differences
        
        return BalanceReconciliationResponse(
            accountId = accountId,
            accountNumber = accountBalance.accountNumber,
            ledgerBalance = ledgerBalance,
            accountBalance = accountBalance.balance,
            difference = difference,
            isReconciled = isReconciled,
            lastReconciliationAt = LocalDateTime.now()
        )
    }
    
    private fun createAccountBalance(accountId: Long, accountNumber: String): AccountBalance {
        val accountBalance = AccountBalance(
            accountId = accountId,
            accountNumber = accountNumber,
            balance = BigDecimal.ZERO,
            availableBalance = BigDecimal.ZERO,
            frozenAmount = BigDecimal.ZERO
        )
        return accountBalanceRepository.save(accountBalance)
    }
    
    private fun publishLedgerEvent(eventType: String, data: Any) {
        val event = mapOf(
            "eventType" to eventType,
            "data" to data,
            "timestamp" to LocalDateTime.now()
        )
        kafkaTemplate.send("ledger-events", event)
    }
}
