package com.petclinic.petservice.outbox;

import com.petclinic.billing.grpc.BillingRequest;
import com.petclinic.petservice.entity.OutboxEvent;
import com.petclinic.petservice.kafka.PetEventPublisher;
import com.petclinic.petservice.repository.OutboxEventRepository;
import com.petclinic.petservice.service.BillingClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OutboxRelayTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 10, 0);

    private OutboxEventRepository repository;
    private PetEventPublisher petEventPublisher;
    private BillingClientService billingClientService;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        petEventPublisher = mock(PetEventPublisher.class);
        billingClientService = mock(BillingClientService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
        relay = new OutboxRelay(repository, petEventPublisher, billingClientService, clock);
    }

    private void pending(OutboxEvent... events) {
        when(repository.lockNextBatch(eq(NOW), anyInt(), anyInt())).thenReturn(List.of(events));
    }

    @Test
    void publishesPetCreatedAndMarksProcessed() throws Exception {
        byte[] payload = {1, 2, 3};
        OutboxEvent event = OutboxEvent.of(OutboxEvent.PET_CREATED, "42", payload, NOW);
        pending(event);

        relay.relayPendingEvents();

        verify(petEventPublisher).publish("42", payload);
        assertEquals(NOW, event.getProcessedAt());
        assertEquals(0, event.getAttempts());
    }

    @Test
    void sendsBillingRequestAndMarksProcessed() {
        BillingRequest request = BillingRequest.newBuilder().setCustomerId("7").setCustomerName("An").build();
        OutboxEvent event = OutboxEvent.of(OutboxEvent.BILLING_ACCOUNT_REQUESTED, "7", request.toByteArray(), NOW);
        pending(event);

        relay.relayPendingEvents();

        verify(billingClientService).createBillingAccount(request);
        assertEquals(NOW, event.getProcessedAt());
    }

    @Test
    void failureKeepsEventPendingWithBackoff() {
        BillingRequest request = BillingRequest.newBuilder().setCustomerId("7").build();
        OutboxEvent event = OutboxEvent.of(OutboxEvent.BILLING_ACCOUNT_REQUESTED, "7", request.toByteArray(), NOW);
        pending(event);
        when(billingClientService.createBillingAccount(any())).thenThrow(new RuntimeException("billing down"));

        relay.relayPendingEvents();

        assertNull(event.getProcessedAt(), "a failed delivery must be retried, not dropped");
        assertEquals(1, event.getAttempts());
        assertEquals("billing down", event.getLastError());
        assertTrue(event.getNextAttemptAt().isAfter(NOW));
    }

    @Test
    void oneFailureDoesNotBlockTheRestOfTheBatch() throws Exception {
        OutboxEvent broken = OutboxEvent.of("UNKNOWN", "1", new byte[0], NOW);
        OutboxEvent ok = OutboxEvent.of(OutboxEvent.PET_CREATED, "2", new byte[]{9}, NOW);
        pending(broken, ok);

        relay.relayPendingEvents();

        assertNull(broken.getProcessedAt());
        assertEquals(NOW, ok.getProcessedAt());
        verify(petEventPublisher).publish("2", new byte[]{9});
    }

    @Test
    void asksOnlyForRowsBelowTheAttemptLimit() {
        pending();

        relay.relayPendingEvents();

        verify(repository).lockNextBatch(NOW, OutboxRelay.MAX_ATTEMPTS, OutboxRelay.BATCH_SIZE);
    }

    @Test
    void backoffGrowsAndIsCapped() {
        OutboxEvent event = OutboxEvent.of(OutboxEvent.PET_CREATED, "1", new byte[0], NOW);

        event.markFailed("x", NOW);
        assertEquals(NOW.plusSeconds(2), event.getNextAttemptAt());
        event.markFailed("x", NOW);
        assertEquals(NOW.plusSeconds(4), event.getNextAttemptAt());
        for (int i = 0; i < 20; i++) {
            event.markFailed("x", NOW);
        }
        assertEquals(NOW.plusMinutes(5), event.getNextAttemptAt());
    }
}
