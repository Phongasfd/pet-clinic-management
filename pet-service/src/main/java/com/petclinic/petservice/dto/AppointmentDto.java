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
        @NotNull(message = "ID của thú cưng không được để trống")
        private Long petId;

        @NotNull(message = "Loại dịch vụ không được để trống")
        private Appointment.ServiceType serviceType;

        @NotNull(message = "Ngày hẹn không được để trống")
        @Future(message = "Ngày hẹn phải ở trong tương lai")
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
    }
}
