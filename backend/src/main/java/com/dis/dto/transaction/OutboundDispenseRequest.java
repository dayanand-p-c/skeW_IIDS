package com.dis.dto.transaction;

import jakarta.validation.constraints.Min;
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
public class OutboundDispenseRequest {

    @NotBlank(message = "Drug ID is required")
    private String drugId;

    @NotNull(message = "Dispense quantity is required")
    @Min(value = 1, message = "Dispense quantity must be at least 1")
    private Integer quantity;

    @Builder.Default
    private String location = "dispensing counter";
}
