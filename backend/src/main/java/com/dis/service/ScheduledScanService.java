package com.dis.service;

import com.dis.dto.transaction.CreateTransactionRequest;
import com.dis.model.Batch;
import com.dis.model.enums.TransactionType;
import com.dis.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledScanService {

    private final RiskEvaluationService riskEvaluationService;
    private final BatchRepository batchRepository;
    private final TransactionService transactionService;
    private final Clock clock;

    /**
     * Incremental risk scan every 15 minutes.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes = 900,000 ms
    public void runFifteenMinuteScan() {
        log.info("Starting scheduled 15-minute risk scan...");
        riskEvaluationService.evaluateAllDrugs();
        log.info("15-minute risk scan completed.");
    }

    /**
     * Full daily scan at 06:00.
     * Automatically writes off expired stock and evaluates all risk profiles.
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void runDailyFullScan() {
        LocalDate today = LocalDate.now(clock);
        log.info("Starting scheduled 06:00 full daily scan for simulated date: {}", today);

        // 1. Detect expired batches with stock and create EXPIRED_WRITE_OFF
        List<Batch> expiredBatches = batchRepository.findExpiredBatchesWithStock(today);
        for (Batch batch : expiredBatches) {
            int expiredQty = batch.getQuantity();
            if (expiredQty > 0) {
                log.warn("Auto-writing off {} expired units for batch {}", expiredQty, batch.getBatchId());
                try {
                    transactionService.recordTransaction(CreateTransactionRequest.builder()
                            .batchId(batch.getBatchId())
                            .type(TransactionType.EXPIRED_WRITE_OFF)
                            .quantity(expiredQty)
                            .location("store room")
                            .build(), "SYSTEM_DAILY_SCAN");
                } catch (Exception e) {
                    log.error("Failed to auto write-off expired batch {}: {}", batch.getBatchId(), e.getMessage());
                }
            }
        }

        // 2. Full risk re-evaluation and snapshot generation
        riskEvaluationService.evaluateAllDrugs();
        log.info("Daily full scan completed.");
    }
}
