package com.dis.controller;

import com.dis.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "High-Performance Analytics and Chart Data Endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/kpis")
    @Operation(summary = "Get high-level inventory KPIs, values, and status counts")
    public ResponseEntity<Map<String, Object>> getKpis(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(analyticsService.getKpis(category));
    }

    @GetMapping("/stock-trend")
    @Operation(summary = "Get daily stock levels over time annotated with ROP and SS")
    public ResponseEntity<Map<String, Object>> getStockTrend(
            @RequestParam(required = false) String drugId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getStockTrend(drugId, from, to));
    }

    @GetMapping("/demand")
    @Operation(summary = "Get daily demand bars with MA7 and MA30 lines and spike days")
    public ResponseEntity<Map<String, Object>> getDemand(
            @RequestParam(required = false) String drugId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getDemandTrend(drugId, from, to));
    }

    @GetMapping("/risk-distribution")
    @Operation(summary = "Get counts of drugs by risk status (SAFE/WATCH/CRITICAL) and category")
    public ResponseEntity<Map<String, Object>> getRiskDistribution() {
        return ResponseEntity.ok(analyticsService.getRiskDistribution());
    }

    @GetMapping("/risk-history")
    @Operation(summary = "Get historical snapshot average risk indices over time")
    public ResponseEntity<Map<String, Object>> getRiskHistory(
            @RequestParam(required = false) String drugId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getRiskHistory(drugId, from, to));
    }

    @GetMapping("/expiry-buckets")
    @Operation(summary = "Get units and values across expiry time buckets")
    public ResponseEntity<Map<String, Object>> getExpiryBuckets(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(analyticsService.getExpiryBuckets(category));
    }

    @GetMapping("/expiry-heatmap")
    @Operation(summary = "Get drug by month units expiring matrix")
    public ResponseEntity<Map<String, Object>> getExpiryHeatmap(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(analyticsService.getExpiryHeatmap(category));
    }

    @GetMapping("/movements")
    @Operation(summary = "Get inbound, outbound, and write-off quantity trends")
    public ResponseEntity<Map<String, Object>> getMovements(
            @RequestParam(required = false, defaultValue = "day") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getMovements(period, from, to));
    }

    @GetMapping("/wastage")
    @Operation(summary = "Get expired wastage value and percentages by month and category")
    public ResponseEntity<Map<String, Object>> getWastageAnalytics() {
        return ResponseEntity.ok(analyticsService.getWastageAnalytics());
    }

    @GetMapping("/abc-xyz")
    @Operation(summary = "Get Pareto ABC and demand variability XYZ matrix items")
    public ResponseEntity<Map<String, Object>> getAbcXyzAnalysis() {
        return ResponseEntity.ok(analyticsService.getAbcXyzAnalysis());
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Get supplier performance, lead times actual vs stated, and shelf life at receipt")
    public ResponseEntity<List<Map<String, Object>>> getSupplierAnalytics() {
        return ResponseEntity.ok(analyticsService.getSupplierAnalytics());
    }

    @GetMapping("/alerts-summary")
    @Operation(summary = "Get alert summary by type, severity, status, and mean time to acknowledge")
    public ResponseEntity<Map<String, Object>> getAlertsSummary() {
        return ResponseEntity.ok(analyticsService.getAlertsSummary());
    }
}
