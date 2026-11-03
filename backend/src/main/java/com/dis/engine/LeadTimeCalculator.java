package com.dis.engine;

import java.util.Collection;

public class LeadTimeCalculator {

    /**
     * Determines lead time L: longest lead_time_days among suppliers of the drug's batches.
     * Defaults to fallbackLeadTime if no batches or suppliers are present.
     */
    public static int calculate(Collection<Integer> supplierLeadTimes, int fallbackLeadTime) {
        if (supplierLeadTimes == null || supplierLeadTimes.isEmpty()) {
            return fallbackLeadTime > 0 ? fallbackLeadTime : 7;
        }

        int maxLeadTime = 0;
        for (Integer lt : supplierLeadTimes) {
            if (lt != null && lt > maxLeadTime) {
                maxLeadTime = lt;
            }
        }
        return maxLeadTime > 0 ? maxLeadTime : (fallbackLeadTime > 0 ? fallbackLeadTime : 7);
    }
}
