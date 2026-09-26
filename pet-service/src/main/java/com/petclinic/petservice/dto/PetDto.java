package com.petclinic.petservice.dto;

import com.petclinic.petservice.entity.Pet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

public class PetDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Tên thú cưng không được để trống")
        private String name;

        @NotNull(message = "Loài thú cưng không được để trống")
        private Pet.Species species;

        private String breed;
        private LocalDate dateOfBirth;
        private Pet.Gender gender;
        private Boolean isNeutered;
        private Double weight;
        private String allergies;
        private String notes;

        @NotNull(message = "ID của chủ sở hữu không được để trống")
        private Long ownerId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private Pet.Species species;
        private String breed;
        private LocalDate dateOfBirth;
        private Pet.Gender gender;
        private Boolean isNeutered;
        private Double weight;
        private String allergies;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private Pet.Species species;
        private String breed;
        private LocalDate dateOfBirth;
        private Pet.Gender gender;
        private Boolean isNeutered;
        private Double weight;
        private String allergies;
        private String notes;
        private Long ownerId;
    }
}
