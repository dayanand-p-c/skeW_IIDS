package com.dis.engine;

import com.dis.model.enums.AlertSeverity;
import com.dis.model.enums.RiskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EngineCalculatorsTest {

    @Test
    @DisplayName("1. DemandCalculator: MA30, MA7, sigma, spike, days of cover")
    void testDemandCalculator() {
        // 30 days of steady 10 units, with a spike of 30 units in the last 7 days
        List<Integer> demand = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            demand.add(10);
        }
        for (int i = 0; i < 7; i++) {
            demand.add(30); // Spike in recent week
        }

        DemandCalculator.DemandResult result = DemandCalculator.calculate(demand, 150, 1.5);
        assertEquals(30.0, result.getMa7(), 0.01);
        // sum = 23*10 + 7*30 = 230 + 210 = 440; ma30 = 440 / 30 = 14.67
        assertEquals(14.67, result.getMa30(), 0.02);
        assertTrue(result.isSpike(), "MA7 (30) > 1.5 * MA30 (22) should trigger spike");
        assertEquals(5.0, result.getDaysOfCover(), 0.01); // 150 / 30 = 5 days of cover

        // Test with empty history
        DemandCalculator.DemandResult empty = DemandCalculator.calculate(List.of(), 50, 1.5);
        assertEquals(0.0, empty.getMa30());
        assertNull(empty.getDaysOfCover());
    }

    @Test
    @DisplayName("2. LeadTimeCalculator: picks maximum supplier lead time")
    void testLeadTimeCalculator() {
        int maxL = LeadTimeCalculator.calculate(List.of(3, 10, 5, 2), 7);
        assertEquals(10, maxL);

        int fallback = LeadTimeCalculator.calculate(List.of(), 14);
        assertEquals(14, fallback);
    }

    @Test
    @DisplayName("3. SafetyStockCalculator: boundary values, Z values, and zero demand")
    void testSafetyStockCalculator() {
        // Zero demand history
        SafetyStockCalculator.StockParameters zero = SafetyStockCalculator.calculate(0.0, 0.0, 7, 0.95);
        assertEquals(0, zero.getSafetyStock());
        assertEquals(0, zero.getReorderPoint());

        // Service level Z factor
        assertEquals(1.28, SafetyStockCalculator.getZFactor(0.90), 0.01);
        assertEquals(1.65, SafetyStockCalculator.getZFactor(0.95), 0.01);
        assertEquals(2.33, SafetyStockCalculator.getZFactor(0.99), 0.01);

        // d=20, sigma=4, L=9, serviceLevel=0.95 (Z=1.65)
        // SS = ceil(1.65 * 4 * sqrt(9)) = ceil(1.65 * 12) = ceil(19.8) = 20
        // ROP = ceil(20 * 9 + 20) = 200
        SafetyStockCalculator.StockParameters params = SafetyStockCalculator.calculate(20.0, 4.0, 9, 0.95);
        assertEquals(20, params.getSafetyStock());
        assertEquals(200, params.getReorderPoint());
    }

    @Test
    @DisplayName("4. ExpiryCalculator: dte <= 90 WARNING, <= 30 CRITICAL, <= 0 expired and wastage risk")
    void testExpiryCalculator() {
        LocalDate today = LocalDate.of(2026, 6, 1);

        // Expired (dte <= 0)
        ExpiryCalculator.ExpiryStatus expired = ExpiryCalculator.evaluate(today, LocalDate.of(2026, 5, 31), 50, 5.0, 90, 30);
        assertTrue(expired.isExpired());
        assertEquals(-1, expired.getDaysToExpiry());

        // Critical (dte = 30)
        ExpiryCalculator.ExpiryStatus critical = ExpiryCalculator.evaluate(today, LocalDate.of(2026, 7, 1), 100, 2.0, 90, 30);
        assertFalse(critical.isExpired());
        assertTrue(critical.isNearExpiry());
        assertEquals(AlertSeverity.CRITICAL, critical.getSeverity());
        // Wastage risk: qty (100) > MA7 (2.0) * dte (30) = 60
        assertTrue(critical.isWastageRisk());

        // Warning (dte = 90)
        ExpiryCalculator.ExpiryStatus warning = ExpiryCalculator.evaluate(today, LocalDate.of(2026, 8, 30), 50, 10.0, 90, 30);
        assertFalse(warning.isExpired());
        assertTrue(warning.isNearExpiry());
        assertEquals(AlertSeverity.WARNING, warning.getSeverity());
        // No wastage: 50 <= 10.0 * 90 (900)
        assertFalse(warning.isWastageRisk());

        // Safe (dte = 120)
        ExpiryCalculator.ExpiryStatus safe = ExpiryCalculator.evaluate(today, LocalDate.of(2026, 9, 29), 50, 10.0, 90, 30);
        assertFalse(safe.isNearExpiry());
        assertNull(safe.getSeverity());
    }

    @Test
    @DisplayName("5. SafetyScoreCalculator: stock risk S, expiry risk E, variability V, and composite index")
    void testSafetyScoreCalculator() {
        // Stock risk S: 1 if stock <= SS; 0 if stock >= 2*ROP; linear in between
        int ss = 20;
        int rop = 100; // 2 * ROP = 200
        assertEquals(1.0, SafetyScoreCalculator.calculateStockRisk(15, ss, rop), 0.001);
        assertEquals(1.0, SafetyScoreCalculator.calculateStockRisk(20, ss, rop), 0.001);
        assertEquals(0.0, SafetyScoreCalculator.calculateStockRisk(200, ss, rop), 0.001);
        assertEquals(0.0, SafetyScoreCalculator.calculateStockRisk(250, ss, rop), 0.001);
        // At stock = 110: (200 - 110) / (200 - 20) = 90 / 180 = 0.5
        assertEquals(0.5, SafetyScoreCalculator.calculateStockRisk(110, ss, rop), 0.001);

        // Expiry risk E: 1 - min(dte, 180)/180; 1 if expired
        assertEquals(1.0, SafetyScoreCalculator.calculateExpiryRisk(-5), 0.001);
        assertEquals(1.0, SafetyScoreCalculator.calculateExpiryRisk(0), 0.001);
        assertEquals(0.5, SafetyScoreCalculator.calculateExpiryRisk(90), 0.001);
        assertEquals(0.0, SafetyScoreCalculator.calculateExpiryRisk(180), 0.001);
        assertEquals(0.0, SafetyScoreCalculator.calculateExpiryRisk(360), 0.001);

        // Variability risk V: min(sigma/mean, 1)
        assertEquals(0.5, SafetyScoreCalculator.calculateVariabilityRisk(5.0, 10.0), 0.001);
        assertEquals(1.0, SafetyScoreCalculator.calculateVariabilityRisk(15.0, 10.0), 0.001);
        assertEquals(0.0, SafetyScoreCalculator.calculateVariabilityRisk(5.0, 0.0), 0.001);

        // Composite index with weights 0.4, 0.4, 0.2
        // S = 0.5, E = 0.5, V = 0.5 -> index = 100 * (0.4*0.5 + 0.4*0.5 + 0.2*0.5) = 50.0 (WATCH)
        double index = SafetyScoreCalculator.calculateIndex(0.5, 0.5, 0.5, 0.4, 0.4, 0.2);
        assertEquals(50.0, index, 0.01);
        assertEquals(RiskStatus.WATCH, SafetyScoreCalculator.resolveStatus(index, 30, 60));
    }

    @Test
    @DisplayName("6. ReorderSuggestionCalculator: suggestion, suppression on NEAR_EXPIRY, shelf life cap")
    void testReorderSuggestionCalculator() {
        // Stock > ROP: no suggestion
        var noReorder = ReorderSuggestionCalculator.calculate(150, 100, 30, 10.0, 7, 365, false);
        assertFalse(noReorder.isSuggested());

        // Stock <= ROP: suggested = ceil(d * (L + 14) + SS - stock)
        // d=10, L=7, SS=30, stock=80 <= ROP (100)
        // target = 10 * (7 + 14) + 30 - 80 = 210 + 30 - 80 = 160
        var reorder = ReorderSuggestionCalculator.calculate(80, 100, 30, 10.0, 7, 365, false);
        assertTrue(reorder.isSuggested());
        assertEquals(160, reorder.getSuggestedQuantity());

        // Suppressed by NEAR_EXPIRY alert
        var suppressed = ReorderSuggestionCalculator.calculate(80, 100, 30, 10.0, 7, 365, true);
        assertFalse(suppressed.isSuggested());
        assertTrue(suppressed.isSuppressedByNearExpiry());
    }
}
