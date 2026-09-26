package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity representing a cage or room for boarding pets.
 */
@Entity
@Table(name = "cages", indexes = {
    @Index(name = "idx_cage_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cage extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier code for the cage (e.g., P101, VIP-01).
     */
    @Column(nullable = false, unique = true)
    private String code;

    /**
     * Type of cage to distinguish features and pricing.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CageType type;

    /**
     * Current availability status of the cage.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CageStatus status;

    /**
     * Daily cost for boarding in this cage.
     */
    @Column(name = "daily_rate", nullable = false)
    private BigDecimal dailyRate;

    public enum CageType {
        VIP, STANDARD, CAT_ROOM
    }

    public enum CageStatus {
        AVAILABLE, OCCUPIED, MAINTENANCE
    }
}
