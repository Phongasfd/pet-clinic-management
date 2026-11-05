package com.petclinic.petservice.dto;

import com.petclinic.petservice.entity.MedicalRecord;
import com.petclinic.petservice.entity.Vaccination;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MedicalDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalRecordResponse {
        private Long id;
        private Long petId;
        private String reason;
        private String symptoms;
        private String diagnosis;
        private String treatment;
        private LocalDate reVisitDate;
        private String notes;
        private LocalDateTime createdAt;

        public static MedicalRecordResponse from(MedicalRecord record) {
            return MedicalRecordResponse.builder()
                    .id(record.getId())
                    .petId(record.getPet().getId())
                    .reason(record.getReason())
                    .symptoms(record.getSymptoms())
                    .diagnosis(record.getDiagnosis())
                    .treatment(record.getTreatment())
                    .reVisitDate(record.getReVisitDate())
                    .notes(record.getNotes())
                    .createdAt(record.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VaccinationResponse {
        private Long id;
        private Long petId;
        private String vaccineName;
        private LocalDate administeredDate;
        private LocalDate nextDueDate;
        private String notes;

        public static VaccinationResponse from(Vaccination vaccination) {
            return VaccinationResponse.builder()
                    .id(vaccination.getId())
                    .petId(vaccination.getPet().getId())
                    .vaccineName(vaccination.getVaccineName())
                    .administeredDate(vaccination.getAdministeredDate())
                    .nextDueDate(vaccination.getNextDueDate())
                    .notes(vaccination.getNotes())
                    .build();
        }
    }
}
