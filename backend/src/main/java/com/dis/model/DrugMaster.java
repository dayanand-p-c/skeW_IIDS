package com.dis.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "drug_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrugMaster {

    @Id
    @Column(name = "drug_id", length = 50, nullable = false)
    private String drugId;

    @Column(nullable = false)
    private String name;

    @Column(length = 100, nullable = false)
    private String category;

    @Column(length = 50, nullable = false)
    private String unit;

    @Column(name = "unit_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "service_level", precision = 4, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal serviceLevel = new BigDecimal("0.95");
}
