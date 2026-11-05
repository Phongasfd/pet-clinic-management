package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Appointments in a time range
    List<Appointment> findByAppointmentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /** Appointments of the pet in the given statuses strictly inside (from, to). */
    @Query("""
            select count(a) from Appointment a
            where a.pet.id = :petId
              and a.status in :statuses
              and a.appointmentDate > :from
              and a.appointmentDate < :to
            """)
    long countInWindow(@Param("petId") Long petId,
                       @Param("statuses") Collection<Appointment.AppointmentStatus> statuses,
                       @Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to);

    /** Locks the appointment so concurrent status changes apply one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);
}
