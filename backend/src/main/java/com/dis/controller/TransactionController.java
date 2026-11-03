package com.dis.controller;

import com.dis.dto.common.PageResponse;
import com.dis.dto.transaction.CreateTransactionRequest;
import com.dis.dto.transaction.DispenseResultDto;
import com.dis.dto.transaction.InboundBatchRequest;
import com.dis.dto.transaction.OutboundDispenseRequest;
import com.dis.dto.transaction.TransactionDto;
import com.dis.exception.BusinessValidationException;
import com.dis.model.enums.TransactionType;
import com.dis.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Stock Transaction and Dispensing Endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/inbound")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    @Operation(summary = "Record inbound batch stock (Warehouse or Admin only; Pharmacist forbidden)")
    public ResponseEntity<TransactionDto> recordInbound(
            @Valid @RequestBody InboundBatchRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.recordInbound(request, authentication.getName()));
    }

    @PostMapping("/dispense")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Dispense drug stock using FEFO order (Pharmacist or Admin only; Warehouse forbidden)")
    public ResponseEntity<DispenseResultDto> dispenseOutbound(
            @Valid @RequestBody OutboundDispenseRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.dispenseOutbound(request, authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST', 'WAREHOUSE', 'ADMIN')")
    @Operation(summary = "Record direct transaction (role restrictions enforced per type)")
    public ResponseEntity<TransactionDto> recordTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPharmacist = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PHARMACIST"));
        boolean isWarehouse = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_WAREHOUSE"));

        // Method-level checks:
        // W cannot create OUTBOUND, EXPIRED_WRITE_OFF, RETURN, ADJUSTMENT
        // P cannot create INBOUND, ADJUSTMENT
        // A can create all
        if (!isAdmin) {
            if (isWarehouse && request.getType() != TransactionType.INBOUND) {
                throw new BusinessValidationException("Warehouse staff can only record INBOUND transactions");
            }
            if (isPharmacist && request.getType() == TransactionType.INBOUND) {
                throw new BusinessValidationException("Pharmacists cannot record INBOUND transactions");
            }
            if (isPharmacist && request.getType() == TransactionType.ADJUSTMENT) {
                throw new BusinessValidationException("Only Administrators can perform stock ADJUSTMENT transactions");
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.recordTransaction(request, authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Search and filter transaction history")
    public ResponseEntity<PageResponse<TransactionDto>> searchTransactions(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String drugId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(transactionService.searchTransactions(batchId, drugId, type, from, to, q, pageable));
    }
}
