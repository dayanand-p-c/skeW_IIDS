package com.dis.dto.drug;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDrugRequest {
    @NotBlank(message = "Drug ID is required")
    private String drugId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Unit is required")
    private String unit;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.01", message = "Unit cost must be positive")
    private BigDecimal unitCost;

    @DecimalMin(value = "0.50", message = "Service level must be between 0.50 and 0.99")
    @DecimalMax(value = "0.99", message = "Service level must be between 0.50 and 0.99")
    @Builder.Default
    private BigDecimal serviceLevel = new BigDecimal("0.95");
}
