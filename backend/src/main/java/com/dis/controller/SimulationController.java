package com.dis.controller;

import com.dis.config.ClockConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/sim")
@RequiredArgsConstructor
@Profile({"sim", "dev", "test"})
@Tag(name = "Simulation", description = "Simulated Clock Management Endpoints (Admin Only)")
public class SimulationController {

    private final ClockConfig clockConfig;
    private final Clock clock;

    @PostMapping("/advance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Advance the simulated clock by N days")
    public ResponseEntity<Map<String, Object>> advanceClock(@RequestParam(defaultValue = "1") long days) {
        clockConfig.advanceDays(days);
        return ResponseEntity.ok(Map.of(
                "advancedDays", days,
                "currentDate", LocalDate.now(clock).toString(),
                "currentInstant", clock.instant().toString()
        ));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset the simulated clock to real-world UTC time")
    public ResponseEntity<Map<String, Object>> resetClock() {
        clockConfig.resetClock();
        return ResponseEntity.ok(Map.of(
                "reset", true,
                "currentDate", LocalDate.now(clock).toString(),
                "currentInstant", clock.instant().toString()
        ));
    }

    @GetMapping("/current")
    @Operation(summary = "Get the current simulated clock time")
    public ResponseEntity<Map<String, Object>> getCurrentClock() {
        return ResponseEntity.ok(Map.of(
                "currentDate", LocalDate.now(clock).toString(),
                "currentInstant", clock.instant().toString()
        ));
    }
}
