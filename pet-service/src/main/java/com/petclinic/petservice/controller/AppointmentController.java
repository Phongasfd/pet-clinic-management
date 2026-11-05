package com.petclinic.petservice.controller;

import com.petclinic.petservice.dto.AppointmentDto;
import com.petclinic.petservice.entity.Appointment;
import com.petclinic.petservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment API", description = "Appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book an appointment (customers: for their own pets; 409 if the pet is already booked within 30 minutes)")
    public AppointmentDto.Response bookAppointment(@RequestBody @Valid AppointmentDto.Request request) {
        return appointmentService.bookAppointment(request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change an appointment's status (customers may only cancel their own)")
    public AppointmentDto.Response updateStatus(
            @PathVariable Long id,
            @RequestParam Appointment.AppointmentStatus newStatus) {
        return appointmentService.updateStatus(id, newStatus);
    }
}
