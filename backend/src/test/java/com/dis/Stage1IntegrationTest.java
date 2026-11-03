package com.dis;

import com.dis.dto.auth.LoginRequest;
import com.dis.dto.auth.LoginResponse;
import com.dis.dto.batch.BatchDto;
import com.dis.dto.batch.CreateBatchRequest;
import com.dis.dto.drug.CreateDrugRequest;
import com.dis.dto.drug.DrugDto;
import com.dis.dto.supplier.CreateSupplierRequest;
import com.dis.dto.supplier.SupplierDto;
import com.dis.model.enums.BatchStatus;
import com.dis.service.AuthService;
import com.dis.service.BatchService;
import com.dis.service.DrugService;
import com.dis.service.SupplierService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Stage1IntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private DrugService drugService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private BatchService batchService;

    @Test
    @DisplayName("Stage 1: Seed users authenticate and receive valid JWT")
    void testAuthLogin() {
        LoginResponse response = authService.login(LoginRequest.builder()
                .username("admin")
                .password("admin123")
                .build());

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole().name());
    }

    @Test
    @DisplayName("Stage 1: Drug, Supplier, and Batch CRUD lifecycle")
    void testDrugSupplierBatchLifecycle() {
        // 1. Create Drug
        DrugDto drug = drugService.createDrug(CreateDrugRequest.builder()
                .drugId("AMOX-500")
                .name("Amoxicillin 500mg")
                .category("antibiotic")
                .unit("capsule")
                .unitCost(new BigDecimal("12.50"))
                .serviceLevel(new BigDecimal("0.95"))
                .build());

        assertNotNull(drug);
        assertEquals("AMOX-500", drug.getDrugId());

        // 2. Create Supplier
        SupplierDto supplier = supplierService.createSupplier(CreateSupplierRequest.builder()
                .supplierId("SUP-01")
                .name("Apex Pharma Supplies")
                .contact("contact@apexpharma.com")
                .leadTimeDays(5)
                .build());

        assertNotNull(supplier);
        assertEquals("SUP-01", supplier.getSupplierId());

        // 3. Create Batch with auto-generated BT-<DRUGID>-<YYMM>-<SEQ> pattern
        LocalDate mfg = LocalDate.of(2026, 1, 15);
        LocalDate exp = LocalDate.of(2027, 1, 15);

        BatchDto batch = batchService.createBatch(CreateBatchRequest.builder()
                .drugId(drug.getDrugId())
                .supplierId(supplier.getSupplierId())
                .manufacturingDate(mfg)
                .expiryDate(exp)
                .build());

        assertNotNull(batch);
        assertTrue(batch.getBatchId().startsWith("BT-AMOX-500-2601-"));
        assertEquals(0, batch.getQuantity());
        assertEquals(BatchStatus.ACTIVE, batch.getStatus());

        // 4. Close Batch
        BatchDto closed = batchService.closeBatch(batch.getBatchId());
        assertEquals(BatchStatus.CLOSED, closed.getStatus());
    }
}
