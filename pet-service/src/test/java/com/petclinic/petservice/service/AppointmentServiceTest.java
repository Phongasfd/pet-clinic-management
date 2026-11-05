package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.AppointmentDto;
import com.petclinic.petservice.entity.Appointment;
import com.petclinic.petservice.entity.Appointment.AppointmentStatus;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.BusinessConflictException;
import com.petclinic.petservice.repository.AppointmentRepository;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.security.AccessGuard;
import com.petclinic.petservice.security.Role;
import com.petclinic.petservice.security.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {

    private static final LocalDateTime SLOT_TIME = LocalDateTime.of(2030, 1, 10, 9, 0);

    private AppointmentRepository appointmentRepository;
    private PetRepository petRepository;
    private AppointmentService service;

    private final Owner owner = Owner.builder().id(1L).userId(100L).build();
    private final Pet pet = Pet.builder().id(10L).owner(owner).build();

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        petRepository = mock(PetRepository.class);
        service = new AppointmentService(appointmentRepository, petRepository, new AccessGuard());
        when(petRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pet));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() {
        TestUsers.clear();
    }

    private static AppointmentDto.Request booking() {
        return AppointmentDto.Request.builder()
                .petId(10L).serviceType(Appointment.ServiceType.VET).appointmentDate(SLOT_TIME).build();
    }

    private Appointment existing(AppointmentStatus status) {
        Appointment appointment = Appointment.builder().id(5L).pet(pet).status(status).appointmentDate(SLOT_TIME).build();
        when(appointmentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(appointment));
        return appointment;
    }

    @Test
    void bookingChecksOnlyActiveAppointmentsInsideTheSlotWindow() {
        TestUsers.as(Role.CUSTOMER, 100L);

        AppointmentDto.Response booked = service.bookAppointment(booking());

        assertEquals(AppointmentStatus.PENDING, booked.getStatus());
        verify(appointmentRepository).countInWindow(eq(10L),
                argThat(statuses -> statuses.size() == 2
                        && statuses.containsAll(java.util.List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED))),
                eq(SLOT_TIME.minusMinutes(30)), eq(SLOT_TIME.plusMinutes(30)));
    }

    @Test
    void overlappingBookingIsRejected() {
        TestUsers.as(Role.RECEPTIONIST, 1L);
        when(appointmentRepository.countInWindow(anyLong(), anyCollection(), any(), any())).thenReturn(1L);

        assertThrows(BusinessConflictException.class, () -> service.bookAppointment(booking()));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void customerCannotBookForSomeoneElsesPet() {
        TestUsers.as(Role.CUSTOMER, 200L);

        assertThrows(AccessDeniedException.class, () -> service.bookAppointment(booking()));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void customerCanCancelButNotConfirmTheirAppointment() {
        TestUsers.as(Role.CUSTOMER, 100L);
        existing(AppointmentStatus.PENDING);

        assertThrows(AccessDeniedException.class, () -> service.updateStatus(5L, AppointmentStatus.CONFIRMED));
        assertEquals(AppointmentStatus.CANCELLED, service.updateStatus(5L, AppointmentStatus.CANCELLED).getStatus());
    }

    @Test
    void customerCannotCancelSomeoneElsesAppointment() {
        TestUsers.as(Role.CUSTOMER, 200L);
        Appointment appointment = existing(AppointmentStatus.PENDING);

        assertThrows(AccessDeniedException.class, () -> service.updateStatus(5L, AppointmentStatus.CANCELLED));
        assertEquals(AppointmentStatus.PENDING, appointment.getStatus());
    }

    @Test
    void completedAppointmentCannotBeReopened() {
        TestUsers.as(Role.VET, 1L);
        Appointment appointment = existing(AppointmentStatus.COMPLETED);

        assertThrows(BusinessConflictException.class, () -> service.updateStatus(5L, AppointmentStatus.PENDING));
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }
}
