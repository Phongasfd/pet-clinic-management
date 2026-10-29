package com.petclinic.petservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * A side effect waiting to be delivered (Transactional Outbox pattern).
 * Rows are inserted in the same DB transaction as the business change, so an event
 * exists if and only if the change committed; {@code OutboxRelay} delivers them afterwards.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    /** Payload: serialized {@code PetEvent}, published to Kafka. */
    public static final String PET_CREATED = "PET_CREATED";
    /** Payload: serialized {@code BillingRequest}, sent to billing-service over gRPC. */
    public static final String BILLING_ACCOUNT_REQUESTED = "BILLING_ACCOUNT_REQUESTED";

    private static final int MAX_ERROR_LENGTH = 2000;
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    /** Kafka message key / log correlation (petId, ownerId). */
    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "payload", nullable = false)
    private byte[] payload;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static OutboxEvent of(String eventType, String aggregateId, byte[] payload, LocalDateTime now) {
        return OutboxEvent.builder()
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(payload)
                .attempts(0)
                .createdAt(now)
                .nextAttemptAt(now)
                .build();
    }

    public void markProcessed(LocalDateTime now) {
        this.processedAt = now;
        this.lastError = null;
    }

    /** Exponential backoff (2s, 4s, 8s ... capped at 5 min) so a dead downstream isn't hammered. */
    public void markFailed(String error, LocalDateTime now) {
        this.attempts++;
        this.lastError = error != null && error.length() > MAX_ERROR_LENGTH
                ? error.substring(0, MAX_ERROR_LENGTH)
                : error;
        long backoffSeconds = Math.min(1L << Math.min(attempts, 20), MAX_BACKOFF.getSeconds());
        this.nextAttemptAt = now.plusSeconds(backoffSeconds);
    }
}
