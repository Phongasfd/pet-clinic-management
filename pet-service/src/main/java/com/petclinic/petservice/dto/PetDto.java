package com.petclinic.petservice.dto;

import com.petclinic.petservice.entity.Pet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

public class PetDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Pet name is required")
        private String name;

        @NotNull(message = "Species is required")
        private Pet.Species species;

        private String breed;
        private LocalDate dateOfBirth;
        private Pet.Gender gender;
        private Boolean isNeutered;

        @Positive(message = "Weight must be greater than 0")
        private Double weight;

        private String allergies;
        private String notes;

        @NotNull(message = "Owner id is required")
        private Long ownerId;
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

        public static Response from(Pet pet) {
            return Response.builder()
                    .id(pet.getId())
                    .name(pet.getName())
                    .species(pet.getSpecies())
                    .breed(pet.getBreed())
                    .dateOfBirth(pet.getDateOfBirth())
                    .gender(pet.getGender())
                    .isNeutered(pet.getIsNeutered())
                    .weight(pet.getWeight())
                    .allergies(pet.getAllergies())
                    .notes(pet.getNotes())
                    .ownerId(pet.getOwner().getId())
                    .build();
        }
    }
}
