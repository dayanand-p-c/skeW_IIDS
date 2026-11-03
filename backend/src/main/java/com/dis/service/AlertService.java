package com.dis.service;

import com.dis.dto.alert.AlertDto;
import com.dis.dto.alert.PatchAlertRequest;
import com.dis.dto.common.PageResponse;
import com.dis.exception.ResourceNotFoundException;
import com.dis.model.Alert;
import com.dis.model.Batch;
import com.dis.model.DrugMaster;
import com.dis.model.enums.AlertSeverity;
import com.dis.model.enums.AlertStatus;
import com.dis.model.enums.AlertType;
import com.dis.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final EmailService emailService;
    private final Clock clock;

    @Transactional
    public Alert raiseOrEscalateDrugAlert(DrugMaster drug, AlertType type, AlertSeverity severity, double score) {
        Optional<Alert> existingOpt = alertRepository.findOpenDrugAlert(drug.getDrugId(), type);
        Instant now = clock.instant();

        if (existingOpt.isPresent()) {
            Alert existing = existingOpt.get();
            boolean isEscalation = existing.getSeverity() == AlertSeverity.WARNING && severity == AlertSeverity.CRITICAL;

            if (isEscalation || score > existing.getRiskScore().doubleValue()) {
                existing.setSeverity(severity);
                existing.setRiskScore(BigDecimal.valueOf(score));
                existing.setUpdatedAt(now);
                Alert saved = alertRepository.save(existing);
                if (isEscalation) {
                    log.info("Escalated drug alert {} for drug {} to CRITICAL", saved.getAlertId(), drug.getDrugId());
                    emailService.sendAlertNotification(saved, true);
                }
                return saved;
            }
            return existing;
        }

        // Create new alert
        Alert newAlert = Alert.builder()
                .alertId("ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .drug(drug)
                .batch(null)
                .type(type)
                .severity(severity)
                .status(AlertStatus.OPEN)
                .riskScore(BigDecimal.valueOf(score))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Alert saved = alertRepository.save(newAlert);
        log.info("Raised new drug alert {} for drug {} (Type: {}, Severity: {})",
                saved.getAlertId(), drug.getDrugId(), type, severity);
        emailService.sendAlertNotification(saved, false);
        return saved;
    }

    @Transactional
    public Alert raiseOrEscalateBatchAlert(DrugMaster drug, Batch batch, AlertType type, AlertSeverity severity, double score) {
        Optional<Alert> existingOpt = alertRepository.findOpenBatchAlert(batch.getBatchId(), type);
        Instant now = clock.instant();

        if (existingOpt.isPresent()) {
            Alert existing = existingOpt.get();
            boolean isEscalation = existing.getSeverity() == AlertSeverity.WARNING && severity == AlertSeverity.CRITICAL;

            if (isEscalation || score > existing.getRiskScore().doubleValue()) {
                existing.setSeverity(severity);
                existing.setRiskScore(BigDecimal.valueOf(score));
                existing.setUpdatedAt(now);
                Alert saved = alertRepository.save(existing);
                if (isEscalation) {
                    log.info("Escalated batch alert {} for batch {} to CRITICAL", saved.getAlertId(), batch.getBatchId());
                    emailService.sendAlertNotification(saved, true);
                }
                return saved;
            }
            return existing;
        }

        // Create new alert
        Alert newAlert = Alert.builder()
                .alertId("ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .drug(drug)
                .batch(batch)
                .type(type)
                .severity(severity)
                .status(AlertStatus.OPEN)
                .riskScore(BigDecimal.valueOf(score))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Alert saved = alertRepository.save(newAlert);
        log.info("Raised new batch alert {} for batch {} (Type: {}, Severity: {})",
                saved.getAlertId(), batch.getBatchId(), type, severity);
        emailService.sendAlertNotification(saved, false);
        return saved;
    }

    @Transactional
    public void autoResolveDrugAlert(String drugId, AlertType type) {
        alertRepository.findOpenDrugAlert(drugId, type).ifPresent(alert -> {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setUpdatedAt(clock.instant());
            alertRepository.save(alert);
            log.info("Auto-resolved drug alert {} for drug {}", alert.getAlertId(), drugId);
        });
    }

    @Transactional
    public void autoResolveBatchAlert(String batchId, AlertType type) {
        alertRepository.findOpenBatchAlert(batchId, type).ifPresent(alert -> {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setUpdatedAt(clock.instant());
            alertRepository.save(alert);
            log.info("Auto-resolved batch alert {} for batch {}", alert.getAlertId(), batchId);
        });
    }

    @Transactional
    public AlertDto patchAlertStatus(String alertId, PatchAlertRequest request) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with ID: " + alertId));

        alert.setStatus(request.getStatus());
        alert.setUpdatedAt(clock.instant());

        if (request.getStatus() == AlertStatus.ACK && alert.getAcknowledgedAt() == null) {
            alert.setAcknowledgedAt(clock.instant());
        }

        Alert updated = alertRepository.save(alert);
        return AlertDto.from(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertDto> findAlerts(AlertStatus status, AlertType type, AlertSeverity severity, Pageable pageable) {
        Page<Alert> page = alertRepository.findAlerts(status, type, severity, pageable);
        return PageResponse.from(page.map(AlertDto::from));
    }
}
