package com.dis.controller;

import com.dis.dto.batch.BatchDto;
import com.dis.dto.batch.CreateBatchRequest;
import com.dis.dto.batch.UpdateBatchRequest;
import com.dis.dto.common.PageResponse;
import com.dis.model.enums.BatchStatus;
import com.dis.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@Tag(name = "Batches", description = "Batch Management Endpoints")
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN', 'PHARMACIST')")
    @Operation(summary = "Get paginated batches with filtering")
    public ResponseEntity<PageResponse<BatchDto>> getAllBatches(
            @RequestParam(required = false) String drugId,
            @RequestParam(required = false) BatchStatus status,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "expiryDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(batchService.searchBatches(drugId, status, query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN', 'PHARMACIST')")
    @Operation(summary = "Get batch by ID")
    public ResponseEntity<BatchDto> getBatchById(@PathVariable("id") String id) {
        return ResponseEntity.ok(batchService.getBatchById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    @Operation(summary = "Create a new batch (Warehouse or Admin only)")
    public ResponseEntity<BatchDto> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.createBatch(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    @Operation(summary = "Update an existing batch details (ignores quantity)")
    public ResponseEntity<BatchDto> updateBatch(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateBatchRequest request) {
        return ResponseEntity.ok(batchService.updateBatch(id, request));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    @Operation(summary = "Close a batch (Warehouse or Admin only)")
    public ResponseEntity<BatchDto> closeBatch(@PathVariable("id") String id) {
        return ResponseEntity.ok(batchService.closeBatch(id));
    }
}
