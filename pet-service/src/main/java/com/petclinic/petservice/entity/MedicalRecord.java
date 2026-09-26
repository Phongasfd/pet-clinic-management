package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity representing a medical record or visit log for a pet.
 */
@Entity
@Table(name = "medical_records", indexes = {
    @Index(name = "idx_medical_record_pet_id", columnList = "pet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The pet associated with this medical record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    /**
     * Primary reason for the clinic visit.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    /**
     * Observed symptoms.
     */
    @Column(columnDefinition = "TEXT")
    private String symptoms;

    /**
     * Doctor's diagnosis.
     */
    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    /**
     * Prescribed treatment or medication.
     */
    @Column(columnDefinition = "TEXT")
    private String treatment;

    /**
     * Scheduled date for the next follow-up visit.
     */
    @Column(name = "re_visit_date")
    private LocalDate reVisitDate;

    /**
     * Additional medical notes.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
}
