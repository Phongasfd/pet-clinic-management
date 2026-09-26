package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a pet in the clinic.
 */
@Entity
@Table(name = "pets", indexes = {
    @Index(name = "idx_pet_name", columnList = "name"),
    @Index(name = "idx_pet_owner_id", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the pet.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Species of the pet (e.g., DOG, CAT).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    /**
     * Specific breed of the pet.
     */
    private String breed;

    /**
     * Pet's date of birth.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Gender of the pet.
     */
    @Enumerated(EnumType.STRING)
    private Gender gender;

    /**
     * Indicates if the pet has been neutered or spayed.
     */
    @Column(name = "is_neutered")
    private Boolean isNeutered;

    /**
     * Current weight of the pet.
     */
    private Double weight;

    /**
     * Known allergies of the pet.
     */
    @Column(columnDefinition = "TEXT")
    private String allergies;

    /**
     * Any additional notes about the pet.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * The owner of this pet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;
    
    /**
     * Medical history records associated with this pet.
     */
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MedicalRecord> medicalRecords = new ArrayList<>();

    public enum Species {
        DOG, CAT, OTHER
    }

    public enum Gender {
        MALE, FEMALE, UNKNOWN
    }
}
