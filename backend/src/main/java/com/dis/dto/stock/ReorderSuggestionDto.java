package com.dis.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSuggestionDto {
    private String drugId;
    private String drugName;
    private String category;
    private String primarySupplierId;
    private String primarySupplierName;
    private int leadTimeDays;
    private long currentStock;
    private int reorderPoint;
    private int safetyStock;
    private int suggestedQuantity;
    private BigDecimal estimatedCost;
    private double ma30;
    private Double daysOfCover;
    private String urgency; // CRITICAL, HIGH, MEDIUM
    private boolean suppressedByNearExpiry;
    private String reason;
}
