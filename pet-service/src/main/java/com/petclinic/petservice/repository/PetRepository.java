package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.Pet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    // Pets of one owner
    List<Pet> findByOwnerId(Long ownerId);

    /**
     * Locks the pet row. Booking an appointment takes this lock first, so two concurrent
     * bookings for the same pet run one after the other and the overlap check can't race.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pet p where p.id = :id")
    Optional<Pet> findByIdForUpdate(@Param("id") Long id);
}
