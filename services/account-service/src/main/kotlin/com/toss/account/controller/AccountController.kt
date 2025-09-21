package com.toss.account.controller

import com.toss.account.dto.*
import com.toss.account.service.AccountService
import com.toss.shared.dto.ApiResponse
import com.toss.shared.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val accountService: AccountService
) {
    
    @PostMapping
    fun createAccount(
        authentication: Authentication,
        @Valid @RequestBody request: CreateAccountRequest
    ): ResponseEntity<ApiResponse<AccountResponse>> {
        val userId = authentication.name.toLong()
        val account = accountService.createAccount(userId, request)
        return ResponseEntity.ok(ApiResponse.success(account))
    }
    
    @GetMapping
    fun getAccounts(authentication: Authentication): ResponseEntity<ApiResponse<List<AccountResponse>>> {
        val userId = authentication.name.toLong()
        val accounts = accountService.getAccountsByUserId(userId)
        return ResponseEntity.ok(ApiResponse.success(accounts))
    }
    
    @GetMapping("/summary")
    fun getAccountSummary(authentication: Authentication): ResponseEntity<ApiResponse<AccountSummaryResponse>> {
        val userId = authentication.name.toLong()
        val summary = accountService.getAccountSummary(userId)
        return ResponseEntity.ok(ApiResponse.success(summary))
    }
    
    @GetMapping("/{accountNumber}")
    fun getAccountByNumber(
        @PathVariable accountNumber: String
    ): ResponseEntity<ApiResponse<AccountResponse>> {
        val account = accountService.getAccountByNumber(accountNumber)
        return ResponseEntity.ok(ApiResponse.success(account))
    }
    
    @GetMapping("/{accountId}/transactions")
    fun getAccountTransactions(
        @PathVariable accountId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PageResponse<AccountTransactionResponse>>> {
        val transactions = accountService.getAccountTransactions(accountId, page, size)
        val response = PageResponse(
            content = transactions.content,
            page = transactions.number,
            size = transactions.size,
            totalElements = transactions.totalElements,
            totalPages = transactions.totalPages,
            first = transactions.isFirst,
            last = transactions.isLast
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    @PutMapping("/{accountId}")
    fun updateAccount(
        authentication: Authentication,
        @PathVariable accountId: Long,
        @Valid @RequestBody request: UpdateAccountRequest
    ): ResponseEntity<ApiResponse<AccountResponse>> {
        val userId = authentication.name.toLong()
        val account = accountService.updateAccount(accountId, userId, request)
        return ResponseEntity.ok(ApiResponse.success(account))
    }
    
    @PostMapping("/transfer")
    fun transfer(
        authentication: Authentication,
        @Valid @RequestBody request: TransferRequest
    ): ResponseEntity<ApiResponse<TransferResponse>> {
        val userId = authentication.name.toLong()
        val transfer = accountService.transfer(request, userId)
        return ResponseEntity.ok(ApiResponse.success(transfer))
    }
}
