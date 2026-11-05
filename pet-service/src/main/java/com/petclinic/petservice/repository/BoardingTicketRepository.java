package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.BoardingTicket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardingTicketRepository extends JpaRepository<BoardingTicket, Long> {

    List<BoardingTicket> findByPetId(Long petId);

    boolean existsByPetIdAndStatus(Long petId, BoardingTicket.BoardingStatus status);

    long countByCageIdAndStatus(Long cageId, BoardingTicket.BoardingStatus status);

    /** Locks the ticket so two concurrent check-outs can't both release the cage. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from BoardingTicket t where t.id = :id")
    Optional<BoardingTicket> findByIdForUpdate(@Param("id") Long id);
}
