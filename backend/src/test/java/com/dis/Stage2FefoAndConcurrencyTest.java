package com.dis;

import com.dis.dto.drug.CreateDrugRequest;
import com.dis.dto.drug.DrugDto;
import com.dis.dto.supplier.CreateSupplierRequest;
import com.dis.dto.supplier.SupplierDto;
import com.dis.dto.transaction.DispenseResultDto;
import com.dis.dto.transaction.InboundBatchRequest;
import com.dis.dto.transaction.OutboundDispenseRequest;
import com.dis.exception.BusinessValidationException;
import com.dis.model.enums.BatchStatus;
import com.dis.repository.BatchRepository;
import com.dis.service.DrugService;
import com.dis.service.SupplierService;
import com.dis.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class Stage2FefoAndConcurrencyTest {

    @Autowired
    private DrugService drugService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BatchRepository batchRepository;

    private DrugDto testDrug;
    private SupplierDto testSupplier;

    @BeforeEach
    void setup() {
        String drugId = "TEST-DRUG-" + System.currentTimeMillis();
        testDrug = drugService.createDrug(CreateDrugRequest.builder()
                .drugId(drugId)
                .name("Paracetamol 500mg Test")
                .category("analgesic")
                .unit("tablet")
                .unitCost(new BigDecimal("5.00"))
                .serviceLevel(new BigDecimal("0.95"))
                .build());

        String supplierId = "TEST-SUP-" + System.currentTimeMillis();
        testSupplier = supplierService.createSupplier(CreateSupplierRequest.builder()
                .supplierId(supplierId)
                .name("Global Pharma Test")
                .contact("test@globalpharma.com")
                .leadTimeDays(4)
                .build());
    }

    @Test
    @DisplayName("Stage 2 Acceptance (1): Pharmacist dispense of 50 units spanning two batches creates two transactions in FEFO order")
    void testFefoMultiBatchSplitting() {
        // Create Batch 1: Expiring in 60 days (Earlier expiry), 30 units
        LocalDate mfg1 = LocalDate.now().minusMonths(2);
        LocalDate exp1 = LocalDate.now().plusDays(60);
        transactionService.recordInbound(InboundBatchRequest.builder()
                .drugId(testDrug.getDrugId())
                .supplierId(testSupplier.getSupplierId())
                .manufacturingDate(mfg1)
                .expiryDate(exp1)
                .quantity(30)
                .build(), "warehouse");

        // Create Batch 2: Expiring in 180 days (Later expiry), 40 units
        LocalDate mfg2 = LocalDate.now().minusMonths(1);
        LocalDate exp2 = LocalDate.now().plusDays(180);
        transactionService.recordInbound(InboundBatchRequest.builder()
                .drugId(testDrug.getDrugId())
                .supplierId(testSupplier.getSupplierId())
                .manufacturingDate(mfg2)
                .expiryDate(exp2)
                .quantity(40)
                .build(), "warehouse");

        // Dispense 50 units
        DispenseResultDto result = transactionService.dispenseOutbound(OutboundDispenseRequest.builder()
                .drugId(testDrug.getDrugId())
                .quantity(50)
                .build(), "pharmacist");

        assertNotNull(result);
        assertEquals(50, result.getDispensedQuantity());
        assertEquals(2, result.getTransactions().size(), "Must create two transaction records for splitting");

        // Transaction 1 must take all 30 from Batch 1 (earliest expiry)
        assertEquals(30, result.getTransactions().get(0).getQuantity());
        // Transaction 2 must take remaining 20 from Batch 2
        assertEquals(20, result.getTransactions().get(1).getQuantity());

        // Batch 1 must now be CLOSED (0 units remaining)
        var b1 = batchRepository.findById(result.getTransactions().get(0).getBatchId()).orElseThrow();
        assertEquals(0, b1.getQuantity());
        assertEquals(BatchStatus.CLOSED, b1.getStatus());

        // Batch 2 must have 20 units remaining and remain ACTIVE
        var b2 = batchRepository.findById(result.getTransactions().get(1).getBatchId()).orElseThrow();
        assertEquals(20, b2.getQuantity());
        assertEquals(BatchStatus.ACTIVE, b2.getStatus());
    }

    @Test
    @DisplayName("Stage 2 Acceptance (2): Two simultaneous dispenses of the last units never oversell")
    void testConcurrentDispenseNoOversell() throws Exception {
        // Inbound a single batch of exactly 30 units
        LocalDate mfg = LocalDate.now().minusMonths(1);
        LocalDate exp = LocalDate.now().plusDays(90);
        transactionService.recordInbound(InboundBatchRequest.builder()
                .drugId(testDrug.getDrugId())
                .supplierId(testSupplier.getSupplierId())
                .manufacturingDate(mfg)
                .expiryDate(exp)
                .quantity(30)
                .build(), "warehouse");

        // Two simultaneous threads each trying to dispense 20 units (total 40 > 30)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                try {
                    transactionService.dispenseOutbound(OutboundDispenseRequest.builder()
                            .drugId(testDrug.getDrugId())
                            .quantity(20)
                            .build(), "pharmacist");
                    return true;
                } catch (BusinessValidationException ex) {
                    return false; // Expected for the losing thread
                }
            }));
        }

        // Release latch to trigger concurrent execution
        startLatch.countDown();

        int successes = 0;
        int failures = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(10, TimeUnit.SECONDS)) {
                successes++;
            } else {
                failures++;
            }
        }

        executor.shutdown();

        // Exactly one must succeed, one must fail with insufficient stock
        assertEquals(1, successes, "Exactly one thread must succeed");
        assertEquals(1, failures, "The second thread must be rejected to prevent overselling");

        // Remaining stock must be exactly 10 units (30 - 20)
        long remaining = batchRepository.getTotalActiveStockByDrug(testDrug.getDrugId());
        assertEquals(10, remaining);
    }
}
