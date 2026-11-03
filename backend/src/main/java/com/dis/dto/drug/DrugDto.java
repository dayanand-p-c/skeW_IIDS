package com.dis.dto.drug;

import com.dis.model.DrugMaster;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugDto {
    private String drugId;
    private String name;
    private String category;
    private String unit;
    private BigDecimal unitCost;
    private BigDecimal serviceLevel;

    // Optional real-time stock and status fields
    private Long currentStock;
    private Long activeBatchesCount;

    public static DrugDto from(DrugMaster drug) {
        return DrugDto.builder()
                .drugId(drug.getDrugId())
                .name(drug.getName())
                .category(drug.getCategory())
                .unit(drug.getUnit())
                .unitCost(drug.getUnitCost())
                .serviceLevel(drug.getServiceLevel())
                .build();
    }
}
