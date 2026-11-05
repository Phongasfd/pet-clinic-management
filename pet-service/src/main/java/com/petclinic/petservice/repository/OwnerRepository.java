package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {

    // Partial match on phone number (paged)
    Page<Owner> findByPhoneNumberContaining(String phoneNumber, Pageable pageable);

    Optional<Owner> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
