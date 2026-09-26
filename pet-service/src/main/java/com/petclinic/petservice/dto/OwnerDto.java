package com.petclinic.petservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

public class OwnerDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Họ tên không được để trống")
        private String fullName;

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải gồm 10 chữ số hợp lệ")
        private String phoneNumber;

        @Email(message = "Email không hợp lệ")
        private String email;

        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String fullName;
        private String phoneNumber;
        private String email;
        private String address;
    }
}
