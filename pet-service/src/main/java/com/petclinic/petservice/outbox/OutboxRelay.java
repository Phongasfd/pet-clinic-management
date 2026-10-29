package com.petclinic.petservice.outbox;

import com.petclinic.billing.grpc.BillingRequest;
import com.petclinic.petservice.entity.OutboxEvent;
import com.petclinic.petservice.kafka.PetEventPublisher;
import com.petclinic.petservice.repository.OutboxEventRepository;
import com.petclinic.petservice.service.BillingClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Delivers committed outbox rows: Kafka events and billing gRPC calls.
 *
 * <p>Delivery is at-least-once (a crash between "sent" and "marked processed" re-sends),
 * so receivers are idempotent: billing-service dedupes on customerId and
 * analytics-service dedupes on the event id.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    static final int BATCH_SIZE = 20;
    /** After this many failures the row is left for an operator (last_error says why). */
    static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final PetEventPublisher petEventPublisher;
    private final BillingClientService billingClientService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void relayPendingEvents() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<OutboxEvent> batch = outboxEventRepository.lockNextBatch(now, MAX_ATTEMPTS, BATCH_SIZE);

        for (OutboxEvent event : batch) {
            try {
                dispatch(event);
                event.markProcessed(now);
            } catch (Exception e) {
                event.markFailed(e.getMessage(), now);
                log.warn("Outbox event id={} type={} failed (attempt {}/{}): {}",
                        event.getId(), event.getEventType(), event.getAttempts(), MAX_ATTEMPTS, e.getMessage());
            }
        }
        // rows are managed entities: changes are flushed when the transaction commits
    }

    private void dispatch(OutboxEvent event) throws Exception {
        switch (event.getEventType()) {
            case OutboxEvent.PET_CREATED ->
                    petEventPublisher.publish(event.getAggregateId(), event.getPayload());
            case OutboxEvent.BILLING_ACCOUNT_REQUESTED ->
                    billingClientService.createBillingAccount(BillingRequest.parseFrom(event.getPayload()));
            default -> throw new IllegalStateException("Unknown outbox event type: " + event.getEventType());
        }
    }
}
