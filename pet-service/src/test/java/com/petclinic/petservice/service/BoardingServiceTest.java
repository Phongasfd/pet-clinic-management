package com.petclinic.petservice.service;

import com.petclinic.petservice.entity.BoardingTicket;
import com.petclinic.petservice.entity.BoardingTicket.BoardingStatus;
import com.petclinic.petservice.entity.Cage;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.BadRequestException;
import com.petclinic.petservice.exception.BusinessConflictException;
import com.petclinic.petservice.exception.CageUnavailableException;
import com.petclinic.petservice.repository.BoardingTicketRepository;
import com.petclinic.petservice.repository.CageRepository;
import com.petclinic.petservice.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Business rules only; the concurrency guarantee is proven against PostgreSQL in BoardingConcurrencyIntegrationTest. */
class BoardingServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 10, 9, 0);

    private BoardingTicketRepository ticketRepository;
    private CageRepository cageRepository;
    private PetRepository petRepository;
    private BoardingService service;

    private final Pet pet = Pet.builder().id(10L).owner(Owner.builder().id(1L).build()).build();

    @BeforeEach
    void setUp() {
        ticketRepository = mock(BoardingTicketRepository.class);
        cageRepository = mock(CageRepository.class);
        petRepository = mock(PetRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2030-01-10T09:00:00Z"), ZoneOffset.UTC);
        service = new BoardingService(ticketRepository, cageRepository, petRepository, clock);
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(ticketRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Cage cage(Cage.CageStatus status) {
        Cage cage = Cage.builder().id(3L).code("STD-01").status(status).build();
        when(cageRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(cage));
        return cage;
    }

    @Test
    void checkInLocksTheCageAndMarksItOccupied() {
        Cage cage = cage(Cage.CageStatus.AVAILABLE);

        var ticket = service.checkIn(10L, 3L, NOW.plusDays(2), null);

        assertEquals(BoardingStatus.ACTIVE, ticket.getStatus());
        assertEquals(NOW, ticket.getCheckInDate());
        assertEquals(Cage.CageStatus.OCCUPIED, cage.getStatus());
        verify(cageRepository).findByIdForUpdate(3L);
        verify(cageRepository, never()).findById(any());
    }

    @Test
    void occupiedCageIsRejected() {
        cage(Cage.CageStatus.OCCUPIED);

        assertThrows(CageUnavailableException.class, () -> service.checkIn(10L, 3L, NOW.plusDays(2), null));
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void petAlreadyBoardingIsRejected() {
        cage(Cage.CageStatus.AVAILABLE);
        when(ticketRepository.existsByPetIdAndStatus(10L, BoardingStatus.ACTIVE)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> service.checkIn(10L, 3L, NOW.plusDays(2), null));
    }

    @Test
    void checkOutDateMustBeInTheFuture() {
        cage(Cage.CageStatus.AVAILABLE);

        assertThrows(BadRequestException.class, () -> service.checkIn(10L, 3L, NOW.minusHours(1), null));
        assertThrows(BadRequestException.class, () -> service.checkIn(10L, 3L, null, null));
    }

    @Test
    void checkOutFreesTheCageOnlyOnce() {
        Cage cage = Cage.builder().id(3L).code("STD-01").status(Cage.CageStatus.OCCUPIED).build();
        BoardingTicket ticket = BoardingTicket.builder().id(7L).pet(pet).cage(cage).status(BoardingStatus.ACTIVE).build();
        when(ticketRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(ticket));

        service.checkOut(7L);
        assertEquals(Cage.CageStatus.AVAILABLE, cage.getStatus());
        assertEquals(NOW, ticket.getActualCheckOutDate());

        // second check-out is a 409, not a 500 (was IllegalStateException)
        assertThrows(BusinessConflictException.class, () -> service.checkOut(7L));
    }
}
