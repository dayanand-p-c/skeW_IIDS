package com.dis.dto.transaction;

import com.dis.model.Transaction;
import com.dis.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long transactionId;
    private String batchId;
    private String drugId;
    private String drugName;
    private TransactionType type;
    private Integer quantity;
    private Instant timestamp;
    private String location;
    private String createdBy;

    public static TransactionDto from(Transaction transaction) {
        return TransactionDto.builder()
                .transactionId(transaction.getTransactionId())
                .batchId(transaction.getBatch() != null ? transaction.getBatch().getBatchId() : null)
                .drugId(transaction.getBatch() != null && transaction.getBatch().getDrug() != null
                        ? transaction.getBatch().getDrug().getDrugId() : null)
                .drugName(transaction.getBatch() != null && transaction.getBatch().getDrug() != null
                        ? transaction.getBatch().getDrug().getName() : null)
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .timestamp(transaction.getTimestamp())
                .location(transaction.getLocation())
                .createdBy(transaction.getCreatedBy())
                .build();
    }
}
