package com.petclinic.petservice.controller;

import com.petclinic.petservice.dto.MedicalDto;
import com.petclinic.petservice.security.ClinicalStaffOnly;
import com.petclinic.petservice.service.MedicalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/pets/{petId}")
@RequiredArgsConstructor
@ClinicalStaffOnly
@Tag(name = "Medical API", description = "Medical records and vaccinations (vets and admins only)")
public class MedicalController {

    private final MedicalService medicalService;

    @PostMapping("/medical-records")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a medical record for a pet")
    public MedicalDto.MedicalRecordResponse createMedicalRecord(
            @PathVariable Long petId,
            @RequestParam String reason,
            @RequestParam(required = false) String symptoms,
            @RequestParam(required = false) String diagnosis,
            @RequestParam(required = false) String treatment,
            @RequestParam(required = false) LocalDate reVisitDate) {
        return medicalService.createMedicalRecord(petId, reason, symptoms, diagnosis, treatment, reVisitDate);
    }

    @PostMapping("/vaccinations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a vaccination")
    public MedicalDto.VaccinationResponse logVaccination(
            @PathVariable Long petId,
            @RequestParam String vaccineName,
            @RequestParam LocalDate administeredDate,
            @RequestParam(required = false) LocalDate nextDueDate) {
        return medicalService.logVaccination(petId, vaccineName, administeredDate, nextDueDate);
    }
}
