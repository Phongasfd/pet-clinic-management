package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    // Tìm danh sách thú cưng theo mã chủ sở hữu
    List<Pet> findByOwnerId(Long ownerId);
}
