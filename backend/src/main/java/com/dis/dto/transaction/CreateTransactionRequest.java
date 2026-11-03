package com.dis.dto.transaction;

import com.dis.model.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Quantity is required")
    private Integer quantity; // Signed for ADJUSTMENT, positive for others

    @NotBlank(message = "Location is required")
    private String location; // "store room" or "dispensing counter"
}
