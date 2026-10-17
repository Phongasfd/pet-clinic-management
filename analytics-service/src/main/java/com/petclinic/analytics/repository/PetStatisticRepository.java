package com.petclinic.analytics.repository;

import com.petclinic.analytics.entity.PetStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetStatisticRepository extends JpaRepository<PetStatistic, Long> {

    Optional<PetStatistic> findBySpecies(String species);
}
