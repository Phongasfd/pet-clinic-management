package com.petclinic.petservice.entity;

import com.petclinic.petservice.entity.Appointment.AppointmentStatus;
import com.petclinic.petservice.entity.SpaTicket.SpaStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusTransitionTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "PENDING,   CONFIRMED, true",
            "PENDING,   CANCELLED, true",
            "PENDING,   COMPLETED, false",  // must be confirmed first
            "PENDING,   PENDING,   false",
            "CONFIRMED, COMPLETED, true",
            "CONFIRMED, CANCELLED, true",
            "CONFIRMED, PENDING,   false",
            "COMPLETED, PENDING,   false",  // a finished visit can't be reopened
            "COMPLETED, CANCELLED, false",
            "CANCELLED, CONFIRMED, false",
    })
    void appointmentTransitions(AppointmentStatus from, AppointmentStatus to, boolean allowed) {
        assertEquals(allowed, from.canTransitionTo(to));
    }

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "RECEIVED,    IN_PROGRESS, true",
            "RECEIVED,    COMPLETED,   false",
            "IN_PROGRESS, COMPLETED,   true",
            "IN_PROGRESS, RECEIVED,    false",
            "COMPLETED,   IN_PROGRESS, false",
    })
    void spaTransitions(SpaStatus from, SpaStatus to, boolean allowed) {
        assertEquals(allowed, from.canTransitionTo(to));
    }
}
