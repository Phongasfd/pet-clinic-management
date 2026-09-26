package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity representing a vaccination record for a pet.
 */
@Entity
@Table(name = "vaccinations", indexes = {
    @Index(name = "idx_vaccination_pet_id", columnList = "pet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vaccination extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The pet receiving the vaccination.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    /**
     * Name of the administered vaccine.
     */
    @Column(name = "vaccine_name", nullable = false)
    private String vaccineName;

    /**
     * Date the vaccine was given.
     */
    @Column(name = "administered_date", nullable = false)
    private LocalDate administeredDate;

    /**
     * Recommended date for the next dose or booster.
     */
    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    /**
     * Additional notes regarding the vaccination.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
}
