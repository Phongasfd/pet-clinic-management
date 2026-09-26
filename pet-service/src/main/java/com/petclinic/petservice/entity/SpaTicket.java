package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entity representing a spa and grooming ticket.
 */
@Entity
@Table(name = "spa_tickets", indexes = {
    @Index(name = "idx_spa_ticket_pet_id", columnList = "pet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaTicket extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The pet receiving spa services.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    /**
     * List of services requested, stored as a JSON array.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> services;

    /**
     * Assessment of the pet's coat condition before grooming.
     */
    @Column(name = "coat_condition")
    private String coatCondition;

    /**
     * Name of the groomer assigned to the ticket.
     */
    @Column(name = "groomer_name")
    private String groomerName;

    /**
     * Current progress status of the spa services.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaStatus status;

    /**
     * Estimated total price for the spa session.
     */
    @Column(name = "estimated_price")
    private BigDecimal estimatedPrice;

    public enum SpaStatus {
        RECEIVED, IN_PROGRESS, COMPLETED
    }
}
