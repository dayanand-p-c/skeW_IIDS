package com.dis.dto.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBatchRequest {
    // If null, can be auto-generated following BT-<DRUGID>-<YYMM>-<SEQ>
    private String batchId;

    @NotBlank(message = "Drug ID is required")
    private String drugId;

    @NotBlank(message = "Supplier ID is required")
    private String supplierId;

    @NotNull(message = "Manufacturing date is required")
    private LocalDate manufacturingDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;
}
