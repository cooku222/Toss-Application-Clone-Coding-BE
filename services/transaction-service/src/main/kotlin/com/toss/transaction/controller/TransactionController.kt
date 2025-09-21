package com.toss.transaction.controller

import com.toss.transaction.dto.*
import com.toss.transaction.entity.TransactionStatus
import com.toss.transaction.entity.TransactionType
import com.toss.transaction.service.TransactionService
import com.toss.shared.dto.ApiResponse
import com.toss.shared.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/transactions")
class TransactionController(
    private val transactionService: TransactionService
) {
    
    @PostMapping
    fun createTransaction(
        authentication: Authentication,
        @Valid @RequestBody request: CreateTransactionRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val userId = authentication.name.toLong()
        val transaction = transactionService.createTransaction(userId, request)
        return ResponseEntity.ok(ApiResponse.success(transaction))
    }
    
    @GetMapping("/{transactionId}")
    fun getTransaction(
        authentication: Authentication,
        @PathVariable transactionId: String
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val userId = authentication.name.toLong()
        val transaction = transactionService.getTransaction(transactionId, userId)
        return ResponseEntity.ok(ApiResponse.success(transaction))
    }
    
    @GetMapping
    fun getTransactions(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: TransactionStatus?,
        @RequestParam(required = false) transactionType: TransactionType?
    ): ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> {
        val userId = authentication.name.toLong()
        val transactions = transactionService.getTransactions(userId, page, size, status, transactionType)
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
    
    @GetMapping("/summary")
    fun getTransactionSummary(authentication: Authentication): ResponseEntity<ApiResponse<TransactionSummaryResponse>> {
        val userId = authentication.name.toLong()
        val summary = transactionService.getTransactionSummary(userId)
        return ResponseEntity.ok(ApiResponse.success(summary))
    }
    
    @PutMapping("/{transactionId}")
    fun updateTransaction(
        authentication: Authentication,
        @PathVariable transactionId: String,
        @Valid @RequestBody request: UpdateTransactionRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val userId = authentication.name.toLong()
        val transaction = transactionService.updateTransaction(transactionId, userId, request)
        return ResponseEntity.ok(ApiResponse.success(transaction))
    }
    
    @PostMapping("/{transactionId}/cancel")
    fun cancelTransaction(
        authentication: Authentication,
        @PathVariable transactionId: String,
        @Valid @RequestBody request: CancelTransactionRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val userId = authentication.name.toLong()
        val transaction = transactionService.cancelTransaction(transactionId, userId, request)
        return ResponseEntity.ok(ApiResponse.success(transaction))
    }
}
