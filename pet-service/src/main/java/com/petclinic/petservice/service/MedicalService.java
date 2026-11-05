package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.MedicalDto;
import com.petclinic.petservice.entity.MedicalRecord;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.entity.Vaccination;
import com.petclinic.petservice.exception.BadRequestException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.MedicalRecordRepository;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.repository.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Vets and admins only, enforced on the controller. */
@Service
@RequiredArgsConstructor
public class MedicalService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;

    @Transactional
    public MedicalDto.MedicalRecordResponse createMedicalRecord(Long petId, String reason, String symptoms,
                                                              String diagnosis, String treatment, LocalDate reVisitDate) {
        Pet pet = findPet(petId);

        MedicalRecord record = MedicalRecord.builder()
                .pet(pet)
                .reason(reason)
                .symptoms(symptoms)
                .diagnosis(diagnosis)
                .treatment(treatment)
                .reVisitDate(reVisitDate)
                .build();

        return MedicalDto.MedicalRecordResponse.from(medicalRecordRepository.save(record));
    }

    @Transactional
    public MedicalDto.VaccinationResponse logVaccination(Long petId, String vaccineName,
                                                         LocalDate administeredDate, LocalDate nextDueDate) {
        if (nextDueDate != null && !nextDueDate.isAfter(administeredDate)) {
            throw new BadRequestException("nextDueDate must be after administeredDate");
        }
        Pet pet = findPet(petId);

        Vaccination vaccination = Vaccination.builder()
                .pet(pet)
                .vaccineName(vaccineName)
                .administeredDate(administeredDate)
                .nextDueDate(nextDueDate)
                .build();

        return MedicalDto.VaccinationResponse.from(vaccinationRepository.save(vaccination));
    }

    private Pet findPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + petId));
    }
}
