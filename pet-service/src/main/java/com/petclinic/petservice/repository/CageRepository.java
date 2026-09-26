package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.Cage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CageRepository extends JpaRepository<Cage, Long> {
    // Tìm các chuồng còn trống theo loại chuồng
    List<Cage> findByTypeAndStatus(Cage.CageType type, Cage.CageStatus status);
}
