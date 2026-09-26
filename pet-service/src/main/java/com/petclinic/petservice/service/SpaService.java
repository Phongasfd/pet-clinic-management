package com.petclinic.petservice.service;

import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.entity.SpaTicket;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.repository.SpaTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpaService {

    private final SpaTicketRepository spaTicketRepository;
    private final PetRepository petRepository;

    @Transactional
    public SpaTicket createSpaTicket(Long petId, List<String> services, String coatCondition, BigDecimal estimatedPrice) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng ID: " + petId));

        SpaTicket ticket = SpaTicket.builder()
                .pet(pet)
                .services(services)
                .coatCondition(coatCondition)
                .estimatedPrice(estimatedPrice)
                .status(SpaTicket.SpaStatus.RECEIVED)
                .build();

        return spaTicketRepository.save(ticket);
    }

    @Transactional
    public SpaTicket updateStatus(Long ticketId, SpaTicket.SpaStatus newStatus) {
        SpaTicket ticket = spaTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu Spa ID: " + ticketId));
        
        ticket.setStatus(newStatus);
        return spaTicketRepository.save(ticket);
    }
}
