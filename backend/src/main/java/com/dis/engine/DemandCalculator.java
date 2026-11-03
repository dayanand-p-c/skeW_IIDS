package com.dis.engine;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public class DemandCalculator {

    @Data
    @Builder
    public static class DemandResult {
        private double ma30;
        private double ma7;
        private double sigma;
        private boolean spike;
        private Double daysOfCover;
    }

    /**
     * Calculates moving averages, standard deviation, spike flag, and days of cover.
     * @param dailyDemand30Days List of daily outbound quantities for the last 30 days (ordered oldest to newest, size up to 30)
     * @param currentStock Current active unexpired stock
     * @param spikeRatio Ratio threshold for spike (default 1.5)
     */
    public static DemandResult calculate(List<Integer> dailyDemand30Days, long currentStock, double spikeRatio) {
        if (dailyDemand30Days == null || dailyDemand30Days.isEmpty()) {
            return DemandResult.builder()
                    .ma30(0.0)
                    .ma7(0.0)
                    .sigma(0.0)
                    .spike(false)
                    .daysOfCover(null)
                    .build();
        }

        int n = dailyDemand30Days.size();
        double sum30 = 0.0;
        for (int qty : dailyDemand30Days) {
            sum30 += qty;
        }
        double ma30 = sum30 / 30.0; // 30-day moving average

        // 7-day average from the most recent 7 days
        int days7 = Math.min(n, 7);
        double sum7 = 0.0;
        for (int i = n - days7; i < n; i++) {
            sum7 += dailyDemand30Days.get(i);
        }
        double ma7 = sum7 / 7.0;

        // Standard deviation of daily demand over 30 days (sample std dev)
        double varianceSum = 0.0;
        for (int qty : dailyDemand30Days) {
            double diff = qty - ma30;
            varianceSum += diff * diff;
        }
        // Account for missing days if list < 30
        if (n < 30) {
            varianceSum += (30 - n) * (ma30 * ma30);
        }
        double sigma = Math.sqrt(varianceSum / 30.0);

        // Spike detection: flag when MA7 > spike.ratio * MA30
        boolean spike = ma30 > 0.0 && ma7 > (spikeRatio * ma30);

        // Days of cover = stock / MA7 (null when MA7 = 0)
        Double daysOfCover = ma7 > 0.0001 ? (double) currentStock / ma7 : null;

        return DemandResult.builder()
                .ma30(ma30)
                .ma7(ma7)
                .sigma(sigma)
                .spike(spike)
                .daysOfCover(daysOfCover)
                .build();
    }
}
