package com.petclinic.petservice.dto;

import com.petclinic.petservice.entity.Appointment;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class AppointmentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull(message = "Pet id is required")
        private Long petId;

        @NotNull(message = "Service type is required")
        private Appointment.ServiceType serviceType;

        @NotNull(message = "Appointment date is required")
        @Future(message = "Appointment date must be in the future")
        private LocalDateTime appointmentDate;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long petId;
        private Appointment.ServiceType serviceType;
        private LocalDateTime appointmentDate;
        private Appointment.AppointmentStatus status;
        private String notes;

        public static Response from(Appointment appointment) {
            return Response.builder()
                    .id(appointment.getId())
                    .petId(appointment.getPet().getId())
                    .serviceType(appointment.getServiceType())
                    .appointmentDate(appointment.getAppointmentDate())
                    .status(appointment.getStatus())
                    .notes(appointment.getNotes())
                    .build();
        }
    }
}
