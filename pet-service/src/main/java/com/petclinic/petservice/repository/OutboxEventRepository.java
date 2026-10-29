package com.petclinic.petservice.repository;

import com.petclinic.petservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // FOR UPDATE SKIP LOCKED: several pet-service instances can run the relay
    // without delivering the same row twice.
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE processed_at IS NULL
              AND attempts < :maxAttempts
              AND next_attempt_at <= :now
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("now") LocalDateTime now,
                                    @Param("maxAttempts") int maxAttempts,
                                    @Param("limit") int limit);
}
