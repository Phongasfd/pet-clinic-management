package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.AppointmentDto;
import com.petclinic.petservice.entity.Appointment;
import com.petclinic.petservice.entity.Appointment.AppointmentStatus;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.BusinessConflictException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.AppointmentRepository;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.security.AccessGuard;
import com.petclinic.petservice.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    /** Length of one appointment slot: two active appointments of a pet must be at least this far apart. */
    static final Duration SLOT = Duration.ofMinutes(30);

    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            Arrays.stream(AppointmentStatus.values()).filter(AppointmentStatus::isActive).toList();

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final AccessGuard accessGuard;

    @Transactional
    public AppointmentDto.Response bookAppointment(AppointmentDto.Request request) {
        // Locking the pet row serialises concurrent bookings for this pet, so the overlap
        // check below sees every appointment committed before it (no check-then-insert race).
        Pet pet = petRepository.findByIdForUpdate(request.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + request.getPetId()));
        accessGuard.checkPetAccess(pet);

        LocalDateTime start = request.getAppointmentDate();
        long overlapping = appointmentRepository.countInWindow(
                pet.getId(), ACTIVE_STATUSES, start.minus(SLOT), start.plus(SLOT));
        if (overlapping > 0) {
            throw new BusinessConflictException("Pet " + pet.getId() + " already has an appointment within "
                    + SLOT.toMinutes() + " minutes of " + start);
        }

        Appointment appointment = Appointment.builder()
                .pet(pet)
                .serviceType(request.getServiceType())
                .appointmentDate(start)
                .status(AppointmentStatus.PENDING)
                .notes(request.getNotes())
                .build();

        return AppointmentDto.Response.from(appointmentRepository.save(appointment));
    }

    /**
     * Staff move appointments through the lifecycle; a customer may only cancel their own.
     * Transitions follow {@link AppointmentStatus#canTransitionTo}: a completed or cancelled
     * appointment can't be reopened.
     */
    @Transactional
    public AppointmentDto.Response updateStatus(Long appointmentId, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        CurrentUser user = accessGuard.currentUser();
        if (!user.isStaff()) {
            // ownership first, so a customer learns nothing about other customers' appointments
            accessGuard.checkPetAccess(appointment.getPet());
            if (newStatus != AppointmentStatus.CANCELLED) {
                throw new AccessDeniedException("Customers can only cancel appointments");
            }
        }

        AppointmentStatus current = appointment.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            throw new BusinessConflictException(
                    "Cannot change appointment " + appointmentId + " from " + current + " to " + newStatus);
        }

        appointment.setStatus(newStatus);
        return AppointmentDto.Response.from(appointment);
    }
}
