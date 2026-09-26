package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.SpaTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaTicketRepository extends JpaRepository<SpaTicket, Long> {
    List<SpaTicket> findByPetId(Long petId);
}
