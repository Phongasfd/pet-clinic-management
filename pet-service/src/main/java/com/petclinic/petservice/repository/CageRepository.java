package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.Cage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CageRepository extends JpaRepository<Cage, Long> {

    // Available cages of a given type
    List<Cage> findByTypeAndStatus(Cage.CageType type, Cage.CageStatus status);

    /**
     * SELECT ... FOR UPDATE: concurrent check-ins for the same cage queue up here, so the
     * "is it AVAILABLE?" check and the "mark OCCUPIED" write happen as one step.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cage c where c.id = :id")
    Optional<Cage> findByIdForUpdate(@Param("id") Long id);
}
