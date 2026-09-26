package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.AppointmentDto;
import com.petclinic.petservice.entity.Appointment;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.AppointmentRepository;
import com.petclinic.petservice.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;

    @Transactional
    public Appointment bookAppointment(AppointmentDto.Request request) {
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng ID: " + request.getPetId()));
                
        Appointment appointment = Appointment.builder()
                .pet(pet)
                .serviceType(request.getServiceType())
                .appointmentDate(request.getAppointmentDate())
                .status(Appointment.AppointmentStatus.PENDING)
                .notes(request.getNotes())
                .build();
                
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment updateStatus(Long appointmentId, Appointment.AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn ID: " + appointmentId));
                
        appointment.setStatus(newStatus);
        return appointmentRepository.save(appointment);
    }
}
