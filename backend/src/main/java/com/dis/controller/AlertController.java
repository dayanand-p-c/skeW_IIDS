package com.dis.controller;

import com.dis.dto.alert.AlertDto;
import com.dis.dto.alert.PatchAlertRequest;
import com.dis.dto.common.PageResponse;
import com.dis.model.enums.AlertSeverity;
import com.dis.model.enums.AlertStatus;
import com.dis.model.enums.AlertType;
import com.dis.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Escalating Alert Management and Notification Endpoints")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'WAREHOUSE')")
    @Operation(summary = "Get alerts with status, type, and severity filtering")
    public ResponseEntity<PageResponse<AlertDto>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertSeverity severity,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(alertService.findAlerts(status, type, severity, pageable));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Acknowledge (ACK) or Resolve an alert")
    public ResponseEntity<AlertDto> patchAlert(
            @PathVariable("id") String id,
            @Valid @RequestBody PatchAlertRequest request) {
        return ResponseEntity.ok(alertService.patchAlertStatus(id, request));
    }
}
