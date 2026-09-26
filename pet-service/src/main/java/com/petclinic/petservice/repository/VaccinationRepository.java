package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {
    // Tìm các mũi tiêm sắp đến hạn trong khoảng thời gian
    List<Vaccination> findByNextDueDateBetween(LocalDate startDate, LocalDate endDate);
}
