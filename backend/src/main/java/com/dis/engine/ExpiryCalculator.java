package com.dis.engine;

import com.dis.model.enums.AlertSeverity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ExpiryCalculator {

    @Data
    @Builder
    public static class ExpiryStatus {
        private long daysToExpiry;
        private boolean expired;
        private boolean nearExpiry;
        private AlertSeverity severity; // null, WARNING, or CRITICAL
        private boolean wastageRisk;
    }

    /**
     * Evaluates batch expiry status, severity, and wastage risk.
     * @param today Simulated current date
     * @param expiryDate Batch expiry date
     * @param quantity Batch current quantity
     * @param ma7 7-day average daily demand
     * @param warnDays Threshold for warning (default 90)
     * @param criticalDays Threshold for critical (default 30)
     */
    public static ExpiryStatus evaluate(LocalDate today,
                                        LocalDate expiryDate,
                                        int quantity,
                                        double ma7,
                                        int warnDays,
                                        int criticalDays) {
        long dte = ChronoUnit.DAYS.between(today, expiryDate);
        boolean expired = dte <= 0;

        AlertSeverity severity = null;
        boolean nearExpiry = false;

        if (dte <= criticalDays && !expired) {
            severity = AlertSeverity.CRITICAL;
            nearExpiry = true;
        } else if (dte <= warnDays && !expired) {
            severity = AlertSeverity.WARNING;
            nearExpiry = true;
        }

        // Wastage risk when quantity > MA7 * dte
        boolean wastageRisk = !expired && dte > 0 && quantity > (ma7 * dte);

        return ExpiryStatus.builder()
                .daysToExpiry(dte)
                .expired(expired)
                .nearExpiry(nearExpiry)
                .severity(severity)
                .wastageRisk(wastageRisk)
                .build();
    }
}
