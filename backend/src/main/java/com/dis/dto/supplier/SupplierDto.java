package com.dis.dto.supplier;

import com.dis.model.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private String supplierId;
    private String name;
    private String contact;
    private Integer leadTimeDays;

    public static SupplierDto from(Supplier supplier) {
        return SupplierDto.builder()
                .supplierId(supplier.getSupplierId())
                .name(supplier.getName())
                .contact(supplier.getContact())
                .leadTimeDays(supplier.getLeadTimeDays())
                .build();
    }
}
