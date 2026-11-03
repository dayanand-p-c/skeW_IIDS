package com.dis.dto.batch;

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
public class UpdateBatchRequest {
    private String supplierId;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private BatchStatus status;
}
