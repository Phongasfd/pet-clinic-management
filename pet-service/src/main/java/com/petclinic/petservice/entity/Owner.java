package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a pet owner.
 */
@Entity
@Table(name = "owners", indexes = {
    @Index(name = "idx_owner_phone", columnList = "phone_number", unique = true),
    @Index(name = "idx_owner_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Owner extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Full name of the pet owner.
     */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /**
     * Unique phone number for contact.
     */
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    /**
     * Optional email address.
     */
    @Column(name = "email")
    private String email;

    /**
     * Physical address of the owner.
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * List of pets belonging to this owner.
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Pet> pets = new ArrayList<>();
}
