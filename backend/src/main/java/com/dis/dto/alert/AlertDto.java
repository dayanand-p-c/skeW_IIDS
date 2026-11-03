package com.dis.dto.alert;

import com.dis.model.Alert;
import com.dis.model.enums.AlertSeverity;
import com.dis.model.enums.AlertStatus;
import com.dis.model.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private String alertId;
    private String drugId;
    private String drugName;
    private String batchId;
    private AlertType type;
    private AlertSeverity severity;
    private AlertStatus status;
    private BigDecimal riskScore;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant acknowledgedAt;

    public static AlertDto from(Alert alert) {
        return AlertDto.builder()
                .alertId(alert.getAlertId())
                .drugId(alert.getDrug() != null ? alert.getDrug().getDrugId() : null)
                .drugName(alert.getDrug() != null ? alert.getDrug().getName() : null)
                .batchId(alert.getBatch() != null ? alert.getBatch().getBatchId() : null)
                .type(alert.getType())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .riskScore(alert.getRiskScore())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .build();
    }
}
