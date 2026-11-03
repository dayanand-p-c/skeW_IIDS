package com.dis.engine;

import lombok.Builder;
import lombok.Data;

public class SafetyStockCalculator {

    @Data
    @Builder
    public static class StockParameters {
        private int safetyStock;
        private int reorderPoint;
        private double zValue;
    }

    /**
     * Obtains Z factor corresponding to the service level.
     * 0.90 = 1.28, 0.95 = 1.65, 0.99 = 2.33.
     */
    public static double getZFactor(double serviceLevel) {
        if (serviceLevel >= 0.99) {
            return 2.33;
        } else if (serviceLevel >= 0.95) {
            // Interpolate between 0.95 (1.65) and 0.99 (2.33)
            return 1.65 + (serviceLevel - 0.95) * ((2.33 - 1.65) / (0.99 - 0.95));
        } else if (serviceLevel >= 0.90) {
            // Interpolate between 0.90 (1.28) and 0.95 (1.65)
            return 1.28 + (serviceLevel - 0.90) * ((1.65 - 1.28) / (0.95 - 0.90));
        } else if (serviceLevel >= 0.50) {
            // Linear approximation down to 0.50 (Z = 0.0)
            return (serviceLevel - 0.50) * (1.28 / 0.40);
        } else {
            return 0.0;
        }
    }

    /**
     * Computes SS = ceil(Z * sigma * sqrt(L)) and ROP = ceil(d * L + SS).
     * If no demand history (d == 0 and sigma == 0): d=0, SS=0, ROP=0.
     */
    public static StockParameters calculate(double d, double sigma, int leadTimeDays, double serviceLevel) {
        if (d <= 0.0 && sigma <= 0.0) {
            return StockParameters.builder()
                    .safetyStock(0)
                    .reorderPoint(0)
                    .zValue(getZFactor(serviceLevel))
                    .build();
        }

        double z = getZFactor(serviceLevel);
        double ssDouble = z * sigma * Math.sqrt(Math.max(1, leadTimeDays));
        int ss = (int) Math.ceil(Math.max(0.0, ssDouble));

        double ropDouble = (d * leadTimeDays) + ss;
        int rop = (int) Math.ceil(Math.max(0.0, ropDouble));

        return StockParameters.builder()
                .safetyStock(ss)
                .reorderPoint(rop)
                .zValue(z)
                .build();
    }
}
