package com.dis.engine;

import lombok.Builder;
import lombok.Data;

public class ReorderSuggestionCalculator {

    @Data
    @Builder
    public static class ReorderSuggestion {
        private boolean suggested;
        private int suggestedQuantity;
        private boolean suppressedByNearExpiry;
        private String reason;
    }

    /**
     * Calculates replenishment suggestion.
     * @param currentStock Current active unexpired stock
     * @param rop Reorder Point
     * @param ss Safety Stock
     * @param d Daily demand (MA30)
     * @param leadTimeDays Lead time L
     * @param avgShelfLifeDays Average shelf life days of batches
     * @param hasNearExpiryAlert True if drug has open NEAR_EXPIRY alert
     */
    public static ReorderSuggestion calculate(
            long currentStock,
            int rop,
            int ss,
            double d,
            int leadTimeDays,
            double avgShelfLifeDays,
            boolean hasNearExpiryAlert) {

        if (hasNearExpiryAlert) {
            return ReorderSuggestion.builder()
                    .suggested(false)
                    .suggestedQuantity(0)
                    .suppressedByNearExpiry(true)
                    .reason("Suppressed: Drug has active NEAR_EXPIRY batch alert")
                    .build();
        }

        if (currentStock > rop) {
            return ReorderSuggestion.builder()
                    .suggested(false)
                    .suggestedQuantity(0)
                    .suppressedByNearExpiry(false)
                    .reason("Stock above Reorder Point (ROP = " + rop + ")")
                    .build();
        }

        // qty = ceil(d * (L + 14) + SS - stock)
        double targetQty = (d * (leadTimeDays + 14)) + ss - currentStock;
        int qty = (int) Math.ceil(Math.max(0.0, targetQty));

        // Capped at d * average shelf life days of the drug's batches
        if (avgShelfLifeDays > 0 && d > 0) {
            int shelfCap = (int) Math.floor(d * avgShelfLifeDays);
            if (shelfCap > 0 && qty > shelfCap) {
                qty = shelfCap;
            }
        }

        return ReorderSuggestion.builder()
                .suggested(qty > 0)
                .suggestedQuantity(Math.max(0, qty))
                .suppressedByNearExpiry(false)
                .reason("Stock (" + currentStock + ") <= ROP (" + rop + ")")
                .build();
    }
}
