package com.dis.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispenseResultDto {
    private String drugId;
    private int requestedQuantity;
    private int dispensedQuantity;
    private List<TransactionDto> transactions;
}
