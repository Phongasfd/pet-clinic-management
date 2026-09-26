package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    // Tìm kiếm gần đúng theo số điện thoại (có phân trang)
    Page<Owner> findByPhoneNumberContaining(String phoneNumber, Pageable pageable);
}
