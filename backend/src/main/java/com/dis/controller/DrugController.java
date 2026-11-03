package com.dis.controller;

import com.dis.dto.common.PageResponse;
import com.dis.dto.drug.CreateDrugRequest;
import com.dis.dto.drug.DrugDto;
import com.dis.dto.drug.UpdateDrugRequest;
import com.dis.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
@Tag(name = "Drugs", description = "Drug Master Management Endpoints")
public class DrugController {

    private final DrugService drugService;

    @GetMapping
    @Operation(summary = "Get paginated list of drugs with search and category filtering")
    public ResponseEntity<PageResponse<DrugDto>> getAllDrugs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(drugService.getAllDrugs(category, query, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get drug by ID")
    public ResponseEntity<DrugDto> getDrugById(@PathVariable("id") String id) {
        return ResponseEntity.ok(drugService.getDrugById(id));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get distinct drug categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(drugService.getAllCategories());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new drug (Admin only)")
    public ResponseEntity<DrugDto> createDrug(@Valid @RequestBody CreateDrugRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(drugService.createDrug(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing drug (Admin only)")
    public ResponseEntity<DrugDto> updateDrug(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateDrugRequest request) {
        return ResponseEntity.ok(drugService.updateDrug(id, request));
    }
}
