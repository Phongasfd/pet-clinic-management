package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.SpaTicketDto;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.entity.SpaTicket;
import com.petclinic.petservice.exception.BusinessConflictException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.repository.SpaTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Staff only, enforced on the controller. */
@Service
@RequiredArgsConstructor
public class SpaService {

    private final SpaTicketRepository spaTicketRepository;
    private final PetRepository petRepository;

    @Transactional
    public SpaTicketDto.Response createSpaTicket(Long petId, List<String> services, String coatCondition,
                                                 BigDecimal estimatedPrice) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + petId));

        SpaTicket ticket = SpaTicket.builder()
                .pet(pet)
                .services(services)
                .coatCondition(coatCondition)
                .estimatedPrice(estimatedPrice)
                .status(SpaTicket.SpaStatus.RECEIVED)
                .build();

        return SpaTicketDto.Response.from(spaTicketRepository.save(ticket));
    }

    @Transactional
    public SpaTicketDto.Response updateStatus(Long ticketId, SpaTicket.SpaStatus newStatus) {
        SpaTicket ticket = spaTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Spa ticket not found: " + ticketId));

        if (!ticket.getStatus().canTransitionTo(newStatus)) {
            throw new BusinessConflictException(
                    "Cannot change spa ticket " + ticketId + " from " + ticket.getStatus() + " to " + newStatus);
        }

        ticket.setStatus(newStatus);
        return SpaTicketDto.Response.from(ticket);
    }
}
