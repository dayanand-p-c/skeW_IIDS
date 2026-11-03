package com.dis.dto.stock;

import com.dis.model.enums.RiskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOverviewDto {
    private String drugId;
    private String drugName;
    private String category;
    private String unit;
    private BigDecimal unitCost;
    private long currentStock;
    private int safetyStock;
    private int reorderPoint;
    private double ma7;
    private double ma30;
    private Double daysOfCover;
    private boolean spike;
    private double riskIndex;
    private RiskStatus status;
}
