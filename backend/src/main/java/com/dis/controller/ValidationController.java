package com.dis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
@Tag(name = "Validation", description = "Replay Experiment Validation Results")
public class ValidationController {

    private final ObjectMapper objectMapper;

    @GetMapping("/results")
    @Operation(summary = "Get simulation and replay experiment validation results (baseline vs DIS system with 95% CI)")
    public ResponseEntity<Map<String, Object>> getValidationResults() {
        try {
            ClassPathResource resource = new ClassPathResource("validation/results.json");
            try (InputStream is = resource.getInputStream()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(is, Map.class);
                return ResponseEntity.ok(data);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load validation results: " + e.getMessage()));
        }
    }
}
