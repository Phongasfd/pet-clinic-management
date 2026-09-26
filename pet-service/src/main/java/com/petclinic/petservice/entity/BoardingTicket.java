package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a boarding reservation or active stay.
 */
@Entity
@Table(name = "boarding_tickets", indexes = {
    @Index(name = "idx_boarding_ticket_pet_id", columnList = "pet_id"),
    @Index(name = "idx_boarding_ticket_cage_id", columnList = "cage_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardingTicket extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The pet staying at the boarding facility.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    /**
     * The specific cage assigned for the stay.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cage_id", nullable = false)
    private Cage cage;

    /**
     * Date and time the pet checked in.
     */
    @Column(name = "check_in_date", nullable = false)
    private LocalDateTime checkInDate;

    /**
     * Scheduled checkout date and time.
     */
    @Column(name = "expected_check_out_date", nullable = false)
    private LocalDateTime expectedCheckOutDate;

    /**
     * Actual checkout date and time.
     */
    @Column(name = "actual_check_out_date")
    private LocalDateTime actualCheckOutDate;

    /**
     * Special instructions for feeding, medication, or care during the stay.
     */
    @Column(name = "special_care_notes", columnDefinition = "TEXT")
    private String specialCareNotes;

    /**
     * Current status of the boarding ticket.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardingStatus status;

    public enum BoardingStatus {
        ACTIVE, COMPLETED
    }
}
