package com.dis.dto.stock;

import com.dis.engine.SafetyScoreCalculator;
import com.dis.model.enums.RiskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugRiskBreakdownDto {
    private String drugId;
    private String drugName;
    private double stockRisk;          // S
    private double expiryRisk;         // E
    private double demandVariability;  // V
    private double riskIndex;
    private RiskStatus status;
    private long currentStock;
    private int safetyStock;
    private int reorderPoint;
    private double ma30;
    private double ma7;
    private double sigma;
    private List<SafetyScoreCalculator.BatchScore> batchScores;
}
