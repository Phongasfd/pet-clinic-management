package com.petclinic.petservice.controller;

import com.petclinic.petservice.entity.MedicalRecord;
import com.petclinic.petservice.entity.Vaccination;
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
@Tag(name = "Medical API", description = "Quản lý bệnh án và tiêm chủng")
public class MedicalController {

    private final MedicalService medicalService;

    @PostMapping("/medical-records")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo hồ sơ bệnh án mới cho thú cưng")
    public MedicalRecord createMedicalRecord(
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
    @Operation(summary = "Ghi nhận lịch tiêm chủng")
    public Vaccination logVaccination(
            @PathVariable Long petId,
            @RequestParam String vaccineName,
            @RequestParam LocalDate administeredDate,
            @RequestParam(required = false) LocalDate nextDueDate) {
        return medicalService.logVaccination(petId, vaccineName, administeredDate, nextDueDate);
    }
}
