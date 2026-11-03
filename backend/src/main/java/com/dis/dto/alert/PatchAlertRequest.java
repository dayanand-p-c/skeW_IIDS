package com.dis.dto.alert;

import com.dis.model.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatchAlertRequest {
    @NotNull(message = "Alert status is required")
    private AlertStatus status; // ACK or RESOLVED
}
