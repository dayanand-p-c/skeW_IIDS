package com.dis;

import com.dis.controller.AnalyticsController;
import com.dis.controller.ValidationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Stage4AnalyticsTest {

    @Autowired
    private AnalyticsController analyticsController;

    @Autowired
    private ValidationController validationController;

    @Test
    @DisplayName("Stage 4: Validation results endpoint returns baseline vs system with 95% confidence intervals")
    void testValidationResults() {
        ResponseEntity<Map<String, Object>> response = validationController.getValidationResults();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("metrics"));
        assertTrue(response.getBody().containsKey("confidenceLevel"));
        assertEquals(0.95, (Double) response.getBody().get("confidenceLevel"), 0.001);
    }

    @Test
    @DisplayName("Stage 4: All 12 analytics endpoints return structured data without errors")
    void testAllAnalyticsEndpoints() {
        // 1. KPIs
        var kpis = analyticsController.getKpis(null);
        assertEquals(200, kpis.getStatusCode().value());
        assertNotNull(kpis.getBody());
        assertTrue(kpis.getBody().containsKey("activeDrugs"));
        assertTrue(kpis.getBody().containsKey("stockValue"));

        // 2. Stock Trend
        var stockTrend = analyticsController.getStockTrend(null, null, null);
        assertEquals(200, stockTrend.getStatusCode().value());
        assertNotNull(stockTrend.getBody());
        assertTrue(stockTrend.getBody().containsKey("dates"));

        // 3. Demand Trend
        var demand = analyticsController.getDemand(null, null, null);
        assertEquals(200, demand.getStatusCode().value());
        assertNotNull(demand.getBody());

        // 4. Risk Distribution
        var riskDist = analyticsController.getRiskDistribution();
        assertEquals(200, riskDist.getStatusCode().value());
        assertNotNull(riskDist.getBody());
        assertTrue(riskDist.getBody().containsKey("topRiskDrugs"));

        // 5. Expiry Buckets
        var expiryBuckets = analyticsController.getExpiryBuckets(null);
        assertEquals(200, expiryBuckets.getStatusCode().value());
        assertNotNull(expiryBuckets.getBody());
        assertTrue(expiryBuckets.getBody().containsKey("labels"));

        // 6. Expiry Heatmap
        var heatmap = analyticsController.getExpiryHeatmap(null);
        assertEquals(200, heatmap.getStatusCode().value());
        assertNotNull(heatmap.getBody());
        assertTrue(heatmap.getBody().containsKey("matrix"));

        // 7. Movements
        var movements = analyticsController.getMovements("day", null, null);
        assertEquals(200, movements.getStatusCode().value());
        assertNotNull(movements.getBody());

        // 8. Wastage
        var wastage = analyticsController.getWastageAnalytics();
        assertEquals(200, wastage.getStatusCode().value());
        assertNotNull(wastage.getBody());

        // 9. ABC-XYZ
        var abcXyz = analyticsController.getAbcXyzAnalysis();
        assertEquals(200, abcXyz.getStatusCode().value());
        assertNotNull(abcXyz.getBody());
        assertTrue(abcXyz.getBody().containsKey("items"));

        // 10. Suppliers
        ResponseEntity<List<Map<String, Object>>> suppliers = analyticsController.getSupplierAnalytics();
        assertEquals(200, suppliers.getStatusCode().value());
        assertNotNull(suppliers.getBody());

        // 11. Alerts Summary
        var alertsSummary = analyticsController.getAlertsSummary();
        assertEquals(200, alertsSummary.getStatusCode().value());
        assertNotNull(alertsSummary.getBody());
    }
}
