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
@Tag(name = "Appointment API", description = "Quản lý lịch hẹn")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đặt lịch hẹn mới")
    public Appointment bookAppointment(@RequestBody @Valid AppointmentDto.Request request) {
        return appointmentService.bookAppointment(request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái lịch hẹn")
    public Appointment updateStatus(
            @PathVariable Long id,
            @RequestParam Appointment.AppointmentStatus newStatus) {
        return appointmentService.updateStatus(id, newStatus);
    }
}
