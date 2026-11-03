package com.dis.service;

import com.dis.dto.batch.CreateBatchRequest;
import com.dis.dto.common.PageResponse;
import com.dis.dto.transaction.CreateTransactionRequest;
import com.dis.dto.transaction.DispenseResultDto;
import com.dis.dto.transaction.InboundBatchRequest;
import com.dis.dto.transaction.OutboundDispenseRequest;
import com.dis.dto.transaction.TransactionDto;
import com.dis.event.DrugStockChangedEvent;
import com.dis.exception.BusinessValidationException;
import com.dis.exception.ResourceNotFoundException;
import com.dis.model.Batch;
import com.dis.model.Transaction;
import com.dis.model.enums.BatchStatus;
import com.dis.model.enums.TransactionType;
import com.dis.repository.BatchRepository;
import com.dis.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BatchRepository batchRepository;
    private final BatchService batchService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Inbound: request carries batch details and quantity.
     * Creates new batch with quantity 0, then records INBOUND transaction to add stock.
     */
    @Transactional
    public TransactionDto recordInbound(InboundBatchRequest request, String createdBy) {
        if (request.getQuantity() <= 0) {
            throw new BusinessValidationException("Inbound quantity must be strictly positive");
        }

        // 1. Create batch with quantity 0
        com.dis.dto.batch.BatchDto createdBatch = batchService.createBatch(CreateBatchRequest.builder()
                .batchId(request.getBatchId())
                .drugId(request.getDrugId())
                .supplierId(request.getSupplierId())
                .manufacturingDate(request.getManufacturingDate())
                .expiryDate(request.getExpiryDate())
                .build());

        Batch batch = batchRepository.findByIdForUpdate(createdBatch.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Failed to lock newly created batch"));

        // 2. Add stock to batch
        batch.setQuantity(request.getQuantity());
        batchRepository.save(batch);

        // 3. Record transaction
        Transaction transaction = Transaction.builder()
                .batch(batch)
                .type(TransactionType.INBOUND)
                .quantity(request.getQuantity())
                .timestamp(clock.instant())
                .location(request.getLocation() != null ? request.getLocation() : "store room")
                .createdBy(createdBy)
                .build();

        Transaction savedTx = transactionRepository.save(transaction);

        // 4. Fire stock change event
        eventPublisher.publishEvent(new DrugStockChangedEvent(this, batch.getDrug().getDrugId()));

        return TransactionDto.from(savedTx);
    }

    /**
     * Outbound: selects unexpired ACTIVE batches in earliest-expiry-first order (FEFO),
     * splits across batches with pessimistic write locks on batch rows so concurrent dispenses never oversell.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DispenseResultDto dispenseOutbound(OutboundDispenseRequest request, String createdBy) {
        if (request.getQuantity() <= 0) {
            throw new BusinessValidationException("Dispense quantity must be strictly positive");
        }

        LocalDate today = LocalDate.now(clock);
        List<Batch> availableBatches = batchRepository.findAvailableBatchesForDispenseWithLock(
                request.getDrugId(),
                BatchStatus.ACTIVE,
                today
        );

        long totalAvailable = availableBatches.stream().mapToLong(Batch::getQuantity).sum();
        if (totalAvailable == 0) {
            throw new BusinessValidationException("Cannot dispense: no unexpired active stock available for drug " + request.getDrugId());
        }

        if (totalAvailable < request.getQuantity()) {
            throw new BusinessValidationException(String.format(
                    "Insufficient stock for drug %s: requested %d, available unexpired %d",
                    request.getDrugId(), request.getQuantity(), totalAvailable));
        }

        int remainingToDispense = request.getQuantity();
        List<TransactionDto> createdTransactions = new ArrayList<>();
        Instant now = clock.instant();

        for (Batch batch : availableBatches) {
            if (remainingToDispense <= 0) {
                break;
            }

            int batchStock = batch.getQuantity();
            int deduct = Math.min(batchStock, remainingToDispense);

            batch.setQuantity(batchStock - deduct);
            if (batch.getQuantity() == 0) {
                batch.setStatus(BatchStatus.CLOSED);
            }
            batchRepository.save(batch);

            Transaction tx = Transaction.builder()
                    .batch(batch)
                    .type(TransactionType.OUTBOUND)
                    .quantity(deduct)
                    .timestamp(now)
                    .location(request.getLocation() != null ? request.getLocation() : "dispensing counter")
                    .createdBy(createdBy)
                    .build();

            Transaction savedTx = transactionRepository.save(tx);
            createdTransactions.add(TransactionDto.from(savedTx));

            remainingToDispense -= deduct;
        }

        // Fire event to re-evaluate risk
        eventPublisher.publishEvent(new DrugStockChangedEvent(this, request.getDrugId()));

        return DispenseResultDto.builder()
                .drugId(request.getDrugId())
                .requestedQuantity(request.getQuantity())
                .dispensedQuantity(request.getQuantity())
                .transactions(createdTransactions)
                .build();
    }

    /**
     * Direct transaction recording (for EXPIRED_WRITE_OFF, RETURN, ADJUSTMENT, or explicit batch transactions).
     */
    @Transactional
    public TransactionDto recordTransaction(CreateTransactionRequest request, String createdBy) {
        Batch batch = batchRepository.findByIdForUpdate(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.getBatchId()));

        int currentQty = batch.getQuantity();
        int newQty;

        switch (request.getType()) {
            case INBOUND -> {
                if (request.getQuantity() <= 0) {
                    throw new BusinessValidationException("Inbound quantity must be positive");
                }
                newQty = currentQty + request.getQuantity();
            }
            case OUTBOUND -> {
                if (request.getQuantity() <= 0) {
                    throw new BusinessValidationException("Outbound quantity must be positive");
                }
                LocalDate today = LocalDate.now(clock);
                if (!batch.getExpiryDate().isAfter(today)) {
                    throw new BusinessValidationException("Cannot dispense from expired batch " + batch.getBatchId());
                }
                if (currentQty < request.getQuantity()) {
                    throw new BusinessValidationException(String.format(
                            "Insufficient stock in batch %s: available %d, requested %d",
                            batch.getBatchId(), currentQty, request.getQuantity()));
                }
                newQty = currentQty - request.getQuantity();
            }
            case EXPIRED_WRITE_OFF -> {
                if (request.getQuantity() <= 0) {
                    throw new BusinessValidationException("Write-off quantity must be positive");
                }
                if (currentQty < request.getQuantity()) {
                    throw new BusinessValidationException(String.format(
                            "Cannot write off %d units; batch only has %d", request.getQuantity(), currentQty));
                }
                newQty = currentQty - request.getQuantity();
            }
            case RETURN -> {
                if (request.getQuantity() <= 0) {
                    throw new BusinessValidationException("Return quantity must be positive");
                }
                newQty = currentQty + request.getQuantity();
            }
            case ADJUSTMENT -> {
                newQty = currentQty + request.getQuantity();
                if (newQty < 0) {
                    throw new BusinessValidationException(String.format(
                            "Adjustment results in negative stock (%d) for batch %s", newQty, batch.getBatchId()));
                }
            }
            default -> throw new BusinessValidationException("Unsupported transaction type: " + request.getType());
        }

        batch.setQuantity(newQty);
        if (newQty == 0 && (request.getType() == TransactionType.OUTBOUND || request.getType() == TransactionType.EXPIRED_WRITE_OFF)) {
            batch.setStatus(BatchStatus.CLOSED);
        } else if (newQty > 0 && batch.getStatus() == BatchStatus.CLOSED && batch.getExpiryDate().isAfter(LocalDate.now(clock))) {
            batch.setStatus(BatchStatus.ACTIVE);
        }
        batchRepository.save(batch);

        Transaction tx = Transaction.builder()
                .batch(batch)
                .type(request.getType())
                .quantity(request.getQuantity())
                .timestamp(clock.instant())
                .location(request.getLocation())
                .createdBy(createdBy)
                .build();

        Transaction savedTx = transactionRepository.save(tx);
        eventPublisher.publishEvent(new DrugStockChangedEvent(this, batch.getDrug().getDrugId()));

        return TransactionDto.from(savedTx);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> searchTransactions(
            String batchId,
            String drugId,
            TransactionType type,
            Instant from,
            Instant to,
            String query,
            Pageable pageable) {

        Page<Transaction> page = transactionRepository.searchTransactions(
                batchId != null && !batchId.isBlank() ? batchId : null,
                drugId != null && !drugId.isBlank() ? drugId : null,
                type,
                from,
                to,
                query != null && !query.isBlank() ? query : null,
                pageable
        );

        return PageResponse.from(page.map(TransactionDto::from));
    }
}
