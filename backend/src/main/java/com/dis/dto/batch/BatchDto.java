package com.dis.dto.batch;

import com.dis.model.Batch;
import com.dis.model.enums.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDto {
    private String batchId;
    private String drugId;
    private String drugName;
    private String supplierId;
    private String supplierName;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private Integer quantity;
    private BatchStatus status;
    private Long daysToExpiry;

    public static BatchDto from(Batch batch, LocalDate today) {
        long dte = today != null ? java.time.temporal.ChronoUnit.DAYS.between(today, batch.getExpiryDate()) : 0;
        return BatchDto.builder()
                .batchId(batch.getBatchId())
                .drugId(batch.getDrug() != null ? batch.getDrug().getDrugId() : null)
                .drugName(batch.getDrug() != null ? batch.getDrug().getName() : null)
                .supplierId(batch.getSupplier() != null ? batch.getSupplier().getSupplierId() : null)
                .supplierName(batch.getSupplier() != null ? batch.getSupplier().getName() : null)
                .manufacturingDate(batch.getManufacturingDate())
                .expiryDate(batch.getExpiryDate())
                .quantity(batch.getQuantity())
                .status(batch.getStatus())
                .daysToExpiry(dte)
                .build();
    }
}
