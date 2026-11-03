package com.dis;

import com.dis.config.ClockConfig;
import com.dis.dto.drug.CreateDrugRequest;
import com.dis.dto.drug.DrugDto;
import com.dis.dto.stock.DrugRiskBreakdownDto;
import com.dis.dto.supplier.CreateSupplierRequest;
import com.dis.dto.supplier.SupplierDto;
import com.dis.dto.transaction.InboundBatchRequest;
import com.dis.model.Alert;
import com.dis.model.SystemConfig;
import com.dis.model.enums.AlertSeverity;
import com.dis.model.enums.AlertType;
import com.dis.repository.AlertRepository;
import com.dis.repository.SystemConfigRepository;
import com.dis.service.DrugService;
import com.dis.service.RiskEvaluationService;
import com.dis.service.StockService;
import com.dis.service.SupplierService;
import com.dis.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class Stage3AlertEscalationTest {

    @Autowired
    private DrugService drugService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private RiskEvaluationService riskEvaluationService;

    @Autowired
    private StockService stockService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private ClockConfig clockConfig;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setupConfig() {
        clockConfig.resetClock();
        configRepository.save(new SystemConfig("weight.stock", "0.4"));
        configRepository.save(new SystemConfig("weight.expiry", "0.4"));
        configRepository.save(new SystemConfig("weight.variability", "0.2"));
        configRepository.save(new SystemConfig("expiry.warn.days", "90"));
        configRepository.save(new SystemConfig("expiry.critical.days", "30"));
        configRepository.save(new SystemConfig("band.watch", "30"));
        configRepository.save(new SystemConfig("band.critical", "60"));
    }

    @Test
    @DisplayName("Stage 3 Acceptance (3): Moving clock 60 days turns a batch from WARNING to CRITICAL and escalates the same alert without creating a second one")
    void testAlertEscalationWithoutDuplicate() {
        LocalDate today = LocalDate.now(clock);

        DrugDto drug = drugService.createDrug(CreateDrugRequest.builder()
                .drugId("ESCALATE-TEST-" + System.currentTimeMillis())
                .name("Cephalexin 500mg")
                .category("antibiotic")
                .unit("capsule")
                .unitCost(new BigDecimal("15.00"))
                .serviceLevel(new BigDecimal("0.95"))
                .build());

        SupplierDto supplier = supplierService.createSupplier(CreateSupplierRequest.builder()
                .supplierId("SUP-ESC-" + System.currentTimeMillis())
                .name("Escalate Supplier")
                .contact("esc@supplier.com")
                .leadTimeDays(5)
                .build());

        // Inbound a batch expiring in 80 days -> should trigger WARNING alert (80 <= 90 warning threshold)
        LocalDate exp = today.plusDays(80);
        var tx = transactionService.recordInbound(InboundBatchRequest.builder()
                .drugId(drug.getDrugId())
                .supplierId(supplier.getSupplierId())
                .manufacturingDate(today.minusMonths(6))
                .expiryDate(exp)
                .quantity(50)
                .build(), "warehouse");

        String batchId = tx.getBatchId();

        // Evaluate risk
        var drugEntity = drugService.getDrugById(drug.getDrugId());
        riskEvaluationService.evaluateAllDrugs();

        // Verify WARNING alert was raised
        List<Alert> alertsPhase1 = alertRepository.findOpenNearExpiryAlertsByDrug(drug.getDrugId());
        assertEquals(1, alertsPhase1.size(), "Should have exactly 1 open NEAR_EXPIRY alert");
        Alert initialAlert = alertsPhase1.get(0);
        assertEquals(AlertSeverity.WARNING, initialAlert.getSeverity());
        String alertId = initialAlert.getAlertId();

        // Advance clock by 60 days -> remaining days = 80 - 60 = 20 days (<= 30 critical threshold)
        clockConfig.advanceDays(60);

        // Re-evaluate risk
        riskEvaluationService.evaluateAllDrugs();

        // Verify alert escalation: MUST be the SAME alert ID, now CRITICAL, and still exactly 1 open alert!
        List<Alert> alertsPhase2 = alertRepository.findOpenNearExpiryAlertsByDrug(drug.getDrugId());
        assertEquals(1, alertsPhase2.size(), "De-duplication: Must NOT create a second alert!");
        Alert escalatedAlert = alertsPhase2.get(0);
        assertEquals(alertId, escalatedAlert.getAlertId(), "Must be the exact same alert row updated");
        assertEquals(AlertSeverity.CRITICAL, escalatedAlert.getSeverity(), "Alert severity must have escalated to CRITICAL");
    }

    @Test
    @DisplayName("Stage 3 Acceptance (4): Changing a weight in Admin changes the scores and the ranking")
    void testWeightChangeAltersScore() {
        DrugDto drug = drugService.createDrug(CreateDrugRequest.builder()
                .drugId("WEIGHT-TEST-" + System.currentTimeMillis())
                .name("Atorvastatin 20mg")
                .category("cardiac")
                .unit("tablet")
                .unitCost(new BigDecimal("8.00"))
                .serviceLevel(new BigDecimal("0.95"))
                .build());

        SupplierDto supplier = supplierService.createSupplier(CreateSupplierRequest.builder()
                .supplierId("SUP-W-" + System.currentTimeMillis())
                .name("Weight Supplier")
                .contact("w@supplier.com")
                .leadTimeDays(5)
                .build());

        // Inbound batch expiring in 90 days (E = 0.5) with 0 stock risk initially
        transactionService.recordInbound(InboundBatchRequest.builder()
                .drugId(drug.getDrugId())
                .supplierId(supplier.getSupplierId())
                .manufacturingDate(LocalDate.now(clock).minusMonths(6))
                .expiryDate(LocalDate.now(clock).plusDays(90))
                .quantity(100)
                .build(), "warehouse");

        // Risk with wE = 0.4
        configRepository.save(new SystemConfig("weight.expiry", "0.4"));
        DrugRiskBreakdownDto score1 = stockService.getDrugRiskBreakdown(drug.getDrugId());

        // Now set wE = 0.8
        configRepository.save(new SystemConfig("weight.expiry", "0.8"));
        DrugRiskBreakdownDto score2 = stockService.getDrugRiskBreakdown(drug.getDrugId());

        assertNotEquals(score1.getRiskIndex(), score2.getRiskIndex(),
                "Updating expiry weight must change composite risk score");
        assertTrue(score2.getRiskIndex() > score1.getRiskIndex(),
                "Higher expiry weight on near-expiry batch must increase risk index");
    }
}
