package com.dis.dto.supplier;

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
public class UpdateSupplierRequest {
    @NotBlank(message = "Supplier name is required")
    private String name;

    @NotBlank(message = "Contact information is required")
    private String contact;

    @NotNull(message = "Lead time in days is required")
    @Min(value = 1, message = "Lead time must be at least 1 day")
    private Integer leadTimeDays;
}
