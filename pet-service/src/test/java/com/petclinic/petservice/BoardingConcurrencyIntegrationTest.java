package com.petclinic.petservice;

import com.petclinic.petservice.entity.BoardingTicket;
import com.petclinic.petservice.entity.BoardingTicket.BoardingStatus;
import com.petclinic.petservice.entity.Cage;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.BusinessConflictException;
import com.petclinic.petservice.exception.CageUnavailableException;
import com.petclinic.petservice.repository.BoardingTicketRepository;
import com.petclinic.petservice.repository.CageRepository;
import com.petclinic.petservice.repository.OwnerRepository;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.service.BoardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The double-booking race, reproduced with real concurrent transactions on PostgreSQL.
 * Before the fix, check-in read the cage status without a lock, so several of these
 * requests saw AVAILABLE and all of them got the same cage.
 */
class BoardingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 16;

    @Autowired private BoardingService boardingService;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private CageRepository cageRepository;
    @Autowired private BoardingTicketRepository ticketRepository;

    private Owner owner;

    @BeforeEach
    void setUp() {
        owner = ownerRepository.save(Owner.builder().fullName("Race owner").phoneNumber(newPhone()).build());
    }

    @Test
    void manyConcurrentCheckInsForOneCage_exactlyOneGetsIt() throws Exception {
        Cage cage = newCage();
        List<Pet> pets = IntStream.range(0, CONCURRENT_REQUESTS).mapToObj(i -> newPet()).toList();

        List<Boolean> results = runConcurrently(pets.stream()
                .map(pet -> (Callable<Boolean>) () -> tryCheckIn(pet.getId(), cage.getId()))
                .toList());

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count(), "exactly one check-in must win");
        assertEquals(Cage.CageStatus.OCCUPIED, cageRepository.findById(cage.getId()).orElseThrow().getStatus());
        assertEquals(1, ticketRepository.countByCageIdAndStatus(cage.getId(), BoardingStatus.ACTIVE));
    }

    @Test
    void samePetIntoDifferentCagesConcurrently_onlyOneStay() throws Exception {
        Pet pet = newPet();
        List<Cage> cages = IntStream.range(0, 8).mapToObj(i -> newCage()).toList();

        List<Boolean> results = runConcurrently(cages.stream()
                .map(cage -> (Callable<Boolean>) () -> tryCheckIn(pet.getId(), cage.getId()))
                .toList());

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        // the losers rolled back, so their cages are still free
        long occupied = cages.stream()
                .map(c -> cageRepository.findById(c.getId()).orElseThrow().getStatus())
                .filter(s -> s == Cage.CageStatus.OCCUPIED)
                .count();
        assertEquals(1, occupied);
    }

    @Test
    void databaseRejectsASecondActiveStayEvenWithoutTheServiceLock() {
        Cage cage = newCage();
        ticketRepository.saveAndFlush(activeTicket(newPet(), cage));

        // uq_boarding_active_cage (V4): the invariant holds for any code path, not just BoardingService
        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(activeTicket(newPet(), cage)));
    }

    @Test
    void cageIsFreeAgainAfterCheckOut() {
        Cage cage = newCage();
        long ticketId = boardingService.checkIn(newPet().getId(), cage.getId(), LocalDateTime.now().plusDays(1), null).getId();

        boardingService.checkOut(ticketId);

        assertTrue(tryCheckIn(newPet().getId(), cage.getId()));
    }

    private boolean tryCheckIn(Long petId, Long cageId) {
        try {
            boardingService.checkIn(petId, cageId, LocalDateTime.now().plusDays(2), null);
            return true;
        } catch (CageUnavailableException | BusinessConflictException expectedForLosers) {
            return false;
        }
    }

    /** Starts every task at the same instant (all threads wait on one latch). */
    private static List<Boolean> runConcurrently(List<Callable<Boolean>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private Cage newCage() {
        return cageRepository.save(Cage.builder()
                .code("RACE-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Cage.CageType.STANDARD)
                .status(Cage.CageStatus.AVAILABLE)
                .dailyRate(new BigDecimal("200000"))
                .build());
    }

    private Pet newPet() {
        return petRepository.save(Pet.builder().name("Racer").species(Pet.Species.DOG).owner(owner).build());
    }

    private static BoardingTicket activeTicket(Pet pet, Cage cage) {
        return BoardingTicket.builder()
                .pet(pet)
                .cage(cage)
                .checkInDate(LocalDateTime.now())
                .expectedCheckOutDate(LocalDateTime.now().plusDays(1))
                .status(BoardingStatus.ACTIVE)
                .build();
    }
}
