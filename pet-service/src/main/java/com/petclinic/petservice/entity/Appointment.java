package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing an appointment scheduling for clinic services.
 */
@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_pet_id", columnList = "pet_id"),
    @Index(name = "idx_appointment_date", columnList = "appointment_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The pet scheduled for the appointment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    /**
     * Category of service requested.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    /**
     * Date and time of the appointment.
     */
    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    /**
     * Current status of the appointment.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    /**
     * Additional notes provided by the owner or staff.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ServiceType {
        VET, SPA, HOTEL
    }

    public enum AppointmentStatus {
        PENDING, CONFIRMED, COMPLETED, CANCELLED
    }
}
