package com.dis.service;

import com.dis.dto.batch.BatchDto;
import com.dis.dto.batch.CreateBatchRequest;
import com.dis.dto.batch.UpdateBatchRequest;
import com.dis.dto.common.PageResponse;
import com.dis.exception.BusinessValidationException;
import com.dis.exception.ResourceNotFoundException;
import com.dis.model.Batch;
import com.dis.model.DrugMaster;
import com.dis.model.Supplier;
import com.dis.model.enums.BatchStatus;
import com.dis.repository.BatchRepository;
import com.dis.repository.DrugMasterRepository;
import com.dis.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final DrugMasterRepository drugRepository;
    private final SupplierRepository supplierRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<BatchDto> searchBatches(String drugId, BatchStatus status, String query, Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        Page<Batch> page = batchRepository.searchBatches(
                drugId != null && !drugId.isBlank() ? drugId : null,
                status,
                query != null && !query.isBlank() ? query : null,
                pageable
        );
        return PageResponse.from(page.map(b -> BatchDto.from(b, today)));
    }

    @Transactional(readOnly = true)
    public BatchDto getBatchById(String batchId) {
        LocalDate today = LocalDate.now(clock);
        Batch batch = batchRepository.findByIdWithDetails(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with ID: " + batchId));
        return BatchDto.from(batch, today);
    }

    @Transactional
    public BatchDto createBatch(CreateBatchRequest request) {
        DrugMaster drug = drugRepository.findById(request.getDrugId())
                .orElseThrow(() -> new ResourceNotFoundException("Drug not found: " + request.getDrugId()));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.getSupplierId()));

        if (request.getExpiryDate().isBefore(request.getManufacturingDate())) {
            throw new BusinessValidationException("Expiry date cannot be before manufacturing date");
        }

        String batchId = request.getBatchId();
        if (batchId == null || batchId.isBlank()) {
            batchId = generateBatchId(drug.getDrugId(), request.getManufacturingDate());
        } else {
            validateBatchIdPattern(batchId, drug.getDrugId());
            if (batchRepository.existsById(batchId)) {
                throw new BusinessValidationException("Batch ID already exists: " + batchId);
            }
        }

        // New batch always starts with quantity 0; inbound transaction adds stock
        Batch batch = Batch.builder()
                .batchId(batchId)
                .drug(drug)
                .supplier(supplier)
                .manufacturingDate(request.getManufacturingDate())
                .expiryDate(request.getExpiryDate())
                .quantity(0)
                .status(BatchStatus.ACTIVE)
                .build();

        Batch saved = batchRepository.save(batch);
        return BatchDto.from(saved, LocalDate.now(clock));
    }

    @Transactional
    public BatchDto updateBatch(String batchId, UpdateBatchRequest request) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        // Note: quantity is strictly NOT updated directly through PUT
        if (request.getSupplierId() != null && !request.getSupplierId().isBlank()) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.getSupplierId()));
            batch.setSupplier(supplier);
        }

        if (request.getManufacturingDate() != null) {
            batch.setManufacturingDate(request.getManufacturingDate());
        }

        if (request.getExpiryDate() != null) {
            if (batch.getManufacturingDate() != null && request.getExpiryDate().isBefore(batch.getManufacturingDate())) {
                throw new BusinessValidationException("Expiry date cannot be before manufacturing date");
            }
            batch.setExpiryDate(request.getExpiryDate());
        }

        if (request.getStatus() != null) {
            batch.setStatus(request.getStatus());
        }

        Batch updated = batchRepository.save(batch);
        return BatchDto.from(updated, LocalDate.now(clock));
    }

    @Transactional
    public BatchDto closeBatch(String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        batch.setStatus(BatchStatus.CLOSED);
        Batch updated = batchRepository.save(batch);
        return BatchDto.from(updated, LocalDate.now(clock));
    }

    public String generateBatchId(String drugId, LocalDate mfgDate) {
        String yymm = mfgDate.format(DateTimeFormatter.ofPattern("yyMM"));
        String prefix = "BT-" + drugId + "-" + yymm + "-";
        int seq = 1;
        while (batchRepository.existsById(String.format("%s%03d", prefix, seq))) {
            seq++;
        }
        return String.format("%s%03d", prefix, seq);
    }

    private void validateBatchIdPattern(String batchId, String drugId) {
        String regex = "^BT-" + java.util.regex.Pattern.quote(drugId) + "-\\d{4}-\\d{3}$";
        if (!batchId.matches(regex)) {
            // If it doesn't match standard regex, check general BT-<DRUGID>-<YYMM>-<SEQ>
            String generalRegex = "^BT-[A-Za-z0-9_-]+-\\d{4}-\\d{3,}$";
            if (!batchId.matches(generalRegex)) {
                throw new BusinessValidationException("Batch ID must match pattern BT-<DRUGID>-<YYMM>-<SEQ>");
            }
        }
    }
}
