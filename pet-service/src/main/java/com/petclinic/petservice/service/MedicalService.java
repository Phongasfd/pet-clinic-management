package com.petclinic.petservice.service;

import com.petclinic.petservice.entity.MedicalRecord;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.entity.Vaccination;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.MedicalRecordRepository;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.repository.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MedicalService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;

    @Transactional
    public MedicalRecord createMedicalRecord(Long petId, String reason, String symptoms, String diagnosis, String treatment, LocalDate reVisitDate) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng ID: " + petId));
                
        MedicalRecord record = MedicalRecord.builder()
                .pet(pet)
                .reason(reason)
                .symptoms(symptoms)
                .diagnosis(diagnosis)
                .treatment(treatment)
                .reVisitDate(reVisitDate)
                .build();
                
        return medicalRecordRepository.save(record);
    }

    @Transactional
    public Vaccination logVaccination(Long petId, String vaccineName, LocalDate administeredDate, LocalDate nextDueDate) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng ID: " + petId));
                
        Vaccination vaccination = Vaccination.builder()
                .pet(pet)
                .vaccineName(vaccineName)
                .administeredDate(administeredDate)
                .nextDueDate(nextDueDate)
                .build();
                
        return vaccinationRepository.save(vaccination);
    }
}
