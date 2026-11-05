package com.petclinic.petservice.dto;

import com.petclinic.petservice.entity.Owner;
import jakarta.validation.constraints.*;
import lombok.*;

public class OwnerDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
        private String phoneNumber;

        @Email(message = "Email is not valid")
        private String email;

        private String address;

        /**
         * Staff only: link the new owner to an existing customer account.
         * Ignored for customers, whose profile is always linked to their own account.
         */
        private Long userId;
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
        private Long userId;

        public static Response from(Owner owner) {
            return Response.builder()
                    .id(owner.getId())
                    .fullName(owner.getFullName())
                    .phoneNumber(owner.getPhoneNumber())
                    .email(owner.getEmail())
                    .address(owner.getAddress())
                    .userId(owner.getUserId())
                    .build();
        }
    }
}
