package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.BoardingTicketDto;
import com.petclinic.petservice.entity.BoardingTicket;
import com.petclinic.petservice.entity.BoardingTicket.BoardingStatus;
import com.petclinic.petservice.entity.Cage;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.BadRequestException;
import com.petclinic.petservice.exception.BusinessConflictException;
import com.petclinic.petservice.exception.CageUnavailableException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.BoardingTicketRepository;
import com.petclinic.petservice.repository.CageRepository;
import com.petclinic.petservice.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Boarding (pet hotel). Staff only, enforced on the controller.
 *
 * <p>Invariants: a cage holds at most one active stay, and a pet is in at most one cage.
 * The old check-in read the cage status and wrote OCCUPIED without a lock, so two
 * concurrent requests could both see AVAILABLE and both get the same cage.
 * Now the cage row is locked ({@code SELECT ... FOR UPDATE}) for the whole check, and partial
 * unique indexes (V4 migration) reject a second active ticket even if a code path skips the lock.
 */
@Service
@RequiredArgsConstructor
public class BoardingService {

    private final BoardingTicketRepository boardingTicketRepository;
    private final CageRepository cageRepository;
    private final PetRepository petRepository;
    private final Clock clock;

    @Transactional
    public BoardingTicketDto.Response checkIn(Long petId, Long cageId, LocalDateTime expectedCheckOutDate, String notes) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (expectedCheckOutDate == null || !expectedCheckOutDate.isAfter(now)) {
            throw new BadRequestException("expectedCheckOutDate must be in the future");
        }

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + petId));
        if (boardingTicketRepository.existsByPetIdAndStatus(petId, BoardingStatus.ACTIVE)) {
            throw new BusinessConflictException("Pet " + petId + " is already checked in");
        }

        // Concurrent check-ins for this cage wait here until the first one commits,
        // then see OCCUPIED below.
        Cage cage = cageRepository.findByIdForUpdate(cageId)
                .orElseThrow(() -> new ResourceNotFoundException("Cage not found: " + cageId));
        if (cage.getStatus() != Cage.CageStatus.AVAILABLE) {
            throw new CageUnavailableException("Cage " + cage.getCode() + " is not available (" + cage.getStatus() + ")");
        }
        cage.setStatus(Cage.CageStatus.OCCUPIED);

        BoardingTicket ticket = BoardingTicket.builder()
                .pet(pet)
                .cage(cage)
                .checkInDate(now)
                .expectedCheckOutDate(expectedCheckOutDate)
                .specialCareNotes(notes)
                .status(BoardingStatus.ACTIVE)
                .build();

        try {
            return BoardingTicketDto.Response.from(boardingTicketRepository.saveAndFlush(ticket));
        } catch (DataIntegrityViolationException e) {
            // uq_boarding_active_pet: the same pet checked into another cage concurrently
            throw new BusinessConflictException("Pet " + petId + " or cage " + cage.getCode() + " already has an active stay");
        }
    }

    @Transactional
    public BoardingTicketDto.Response checkOut(Long ticketId) {
        // locked, so two concurrent check-outs can't both succeed
        BoardingTicket ticket = boardingTicketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Boarding ticket not found: " + ticketId));

        if (ticket.getStatus() == BoardingStatus.COMPLETED) {
            throw new BusinessConflictException("Boarding ticket " + ticketId + " is already checked out");
        }

        ticket.setStatus(BoardingStatus.COMPLETED);
        ticket.setActualCheckOutDate(LocalDateTime.now(clock));
        ticket.getCage().setStatus(Cage.CageStatus.AVAILABLE);

        return BoardingTicketDto.Response.from(ticket);
    }
}
