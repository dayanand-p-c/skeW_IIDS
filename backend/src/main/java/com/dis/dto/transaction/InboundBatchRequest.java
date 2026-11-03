package com.dis.dto.transaction;

import jakarta.validation.constraints.Min;
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
public class InboundBatchRequest {
    // Optional: if null, auto-generated using pattern BT-<DRUGID>-<YYMM>-<SEQ>
    private String batchId;

    @NotBlank(message = "Drug ID is required")
    private String drugId;

    @NotBlank(message = "Supplier ID is required")
    private String supplierId;

    @NotNull(message = "Manufacturing date is required")
    private LocalDate manufacturingDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Inbound quantity must be at least 1")
    private Integer quantity;

    @Builder.Default
    private String location = "store room";
}
