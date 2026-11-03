package com.dis.controller;

import com.dis.dto.stock.DrugRiskBreakdownDto;
import com.dis.dto.stock.ReorderSuggestionDto;
import com.dis.dto.stock.StockOverviewDto;
import com.dis.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Stock & Reorder", description = "Stock Overview, Risk Breakdowns, and Reorder Suggestions")
public class StockController {

    private final StockService stockService;

    @GetMapping("/stock/overview")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'WAREHOUSE')")
    @Operation(summary = "Get current stock overview with computed ROP, SS, and safety indices")
    public ResponseEntity<List<StockOverviewDto>> getStockOverview(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(stockService.getStockOverview(category));
    }

    @GetMapping("/stock/{drugId}/risk")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Get detailed S, E, V score breakdown and batch contributions for a drug")
    public ResponseEntity<DrugRiskBreakdownDto> getDrugRisk(@PathVariable("drugId") String drugId) {
        return ResponseEntity.ok(stockService.getDrugRiskBreakdown(drugId));
    }

    @GetMapping("/reorder/suggestions")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Get automated reorder suggestions (suppressed if open NEAR_EXPIRY alert exists)")
    public ResponseEntity<List<ReorderSuggestionDto>> getReorderSuggestions() {
        return ResponseEntity.ok(stockService.getReorderSuggestions());
    }
}
