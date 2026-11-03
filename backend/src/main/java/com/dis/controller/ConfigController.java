package com.dis.controller;

import com.dis.model.SystemConfig;
import com.dis.repository.SystemConfigRepository;
import com.dis.service.RiskEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "Configuration", description = "System Configuration and Score Weight Management (Admin only)")
public class ConfigController {

    private final SystemConfigRepository configRepository;
    private final RiskEvaluationService riskEvaluationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all system configuration keys and values")
    public ResponseEntity<Map<String, String>> getAllConfig() {
        List<SystemConfig> list = configRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SystemConfig sc : list) {
            map.put(sc.getConfigKey(), sc.getConfigValue());
        }
        return ResponseEntity.ok(map);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update system configurations and recalculate safety scores")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            SystemConfig config = SystemConfig.builder()
                    .configKey(entry.getKey())
                    .configValue(entry.getValue())
                    .build();
            configRepository.save(config);
        }

        // Re-evaluate risk across all drugs when weights or thresholds change
        riskEvaluationService.evaluateAllDrugs();

        return getAllConfig();
    }
}
