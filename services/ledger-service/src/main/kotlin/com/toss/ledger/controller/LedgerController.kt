package com.toss.ledger.controller

import com.toss.ledger.dto.*
import com.toss.ledger.entity.LedgerEntryStatus
import com.toss.ledger.entity.LedgerEntryType
import com.toss.ledger.service.LedgerService
import com.toss.shared.dto.ApiResponse
import com.toss.shared.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ledger")
class LedgerController(
    private val ledgerService: LedgerService
) {
    
    @PostMapping("/entries")
    fun createLedgerEntry(
        @Valid @RequestBody request: CreateLedgerEntryRequest
    ): ResponseEntity<ApiResponse<LedgerEntryResponse>> {
        val entry = ledgerService.createLedgerEntry(request)
        return ResponseEntity.ok(ApiResponse.success(entry))
    }
    
    @GetMapping("/entries/{accountId}")
    fun getLedgerEntries(
        @PathVariable accountId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) entryType: LedgerEntryType?,
        @RequestParam(required = false) status: LedgerEntryStatus?
    ): ResponseEntity<ApiResponse<PageResponse<LedgerEntryResponse>>> {
        val entries = ledgerService.getLedgerEntries(accountId, page, size, entryType, status)
        val response = PageResponse(
            content = entries.content,
            page = entries.number,
            size = entries.size,
            totalElements = entries.totalElements,
            totalPages = entries.totalPages,
            first = entries.isFirst,
            last = entries.isLast
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    @GetMapping("/balance/{accountId}")
    fun getAccountBalance(
        @PathVariable accountId: Long
    ): ResponseEntity<ApiResponse<AccountBalanceResponse>> {
        val balance = ledgerService.getAccountBalance(accountId)
        return ResponseEntity.ok(ApiResponse.success(balance))
    }
    
    @GetMapping("/summary/{accountId}")
    fun getLedgerSummary(
        @PathVariable accountId: Long
    ): ResponseEntity<ApiResponse<LedgerSummaryResponse>> {
        val summary = ledgerService.getLedgerSummary(accountId)
        return ResponseEntity.ok(ApiResponse.success(summary))
    }
    
    @PostMapping("/entries/{entryId}/reverse")
    fun reverseLedgerEntry(
        @PathVariable entryId: Long,
        @Valid @RequestBody request: ReverseLedgerEntryRequest
    ): ResponseEntity<ApiResponse<LedgerEntryResponse>> {
        val entry = ledgerService.reverseLedgerEntry(entryId, request)
        return ResponseEntity.ok(ApiResponse.success(entry))
    }
    
    @PostMapping("/balance/adjust")
    fun adjustBalance(
        @Valid @RequestBody request: BalanceAdjustmentRequest
    ): ResponseEntity<ApiResponse<AccountBalanceResponse>> {
        val balance = ledgerService.adjustBalance(request)
        return ResponseEntity.ok(ApiResponse.success(balance))
    }
    
    @PostMapping("/reconcile/{accountId}")
    fun reconcileBalance(
        @PathVariable accountId: Long
    ): ResponseEntity<ApiResponse<BalanceReconciliationResponse>> {
        val reconciliation = ledgerService.reconcileBalance(accountId)
        return ResponseEntity.ok(ApiResponse.success(reconciliation))
    }
}
