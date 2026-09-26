package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.BoardingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardingTicketRepository extends JpaRepository<BoardingTicket, Long> {
    List<BoardingTicket> findByPetId(Long petId);
}
