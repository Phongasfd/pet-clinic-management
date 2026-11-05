package com.petclinic.petservice;

import com.petclinic.billing.grpc.BillingRequest;
import com.petclinic.billing.grpc.BillingResponse;
import com.petclinic.events.PetEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.mockito.ArgumentMatcher;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.petclinic.petservice.security.Role.RECEPTIONIST;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** The outbox end to end: business write → outbox row → relay → Kafka / billing gRPC, with retries. */
class OutboxFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EmbeddedKafkaBroker kafka;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void createdPetIsPublishedToKafkaAfterCommit() throws Exception {
        long ownerId = createOwnerAs(RECEPTIONIST, 1, "Walk-in " + UUID.randomUUID());
        long petId = addPetAs(RECEPTIONIST, 1, ownerId);

        Map<String, Object> props = KafkaTestUtils.consumerProps("outbox-it-" + UUID.randomUUID(), "false", kafka);
        try (Consumer<String, byte[]> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new ByteArrayDeserializer()).createConsumer()) {
            consumer.subscribe(List.of("pet-events"));

            AtomicReference<ConsumerRecord<String, byte[]>> found = new AtomicReference<>();
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                for (ConsumerRecord<String, byte[]> record : consumer.poll(Duration.ofMillis(500))) {
                    if (String.valueOf(petId).equals(record.key())) { // keyed by petId: per-pet ordering
                        found.set(record);
                    }
                }
                return found.get() != null;
            });

            PetEvent event = PetEvent.parseFrom(found.get().value());
            assertEquals(petId, event.getPetId());
            assertEquals(ownerId, event.getOwnerId());
            assertEquals("DOG", event.getSpecies());
            assertFalse(event.getEventId().isBlank(), "consumers dedupe on eventId");
        }

        await().atMost(Duration.ofSeconds(10)).until(() -> processedAt("PET_CREATED", petId) != null);
    }

    @Test
    void billingOutageIsRetriedUntilBillingRecovers() throws Exception {
        String name = "Billing outage " + UUID.randomUUID();
        ArgumentMatcher<BillingRequest> forThisOwner = r -> r != null && name.equals(r.getCustomerName());
        // first call fails (billing-service down), the retry succeeds
        doThrow(new RuntimeException("billing-service unavailable"))
                .doReturn(BillingResponse.getDefaultInstance())
                .when(billingClientService).createBillingAccount(argThat(forThisOwner));

        // the HTTP request succeeds even though billing is down: it does not wait for billing
        long ownerId = createOwnerAs(RECEPTIONIST, 1, name);

        await().atMost(Duration.ofSeconds(10)).until(() -> attempts(ownerId) >= 1);
        assertTrue(lastError(ownerId).contains("billing-service unavailable"));

        // backoff after one failure is 2s; then the relay delivers it
        await().atMost(Duration.ofSeconds(20)).until(() -> processedAt("BILLING_ACCOUNT_REQUESTED", ownerId) != null);
        assertEquals(1, attempts(ownerId));
        assertNull(lastError(ownerId));
        verify(billingClientService, times(2)).createBillingAccount(argThat(forThisOwner));
    }

    private Object processedAt(String type, long aggregateId) {
        return jdbc.queryForObject(
                "select processed_at from outbox_events where event_type = ? and aggregate_id = ?",
                Object.class, type, String.valueOf(aggregateId));
    }

    private int attempts(long ownerId) {
        return jdbc.queryForObject(
                "select attempts from outbox_events where event_type = 'BILLING_ACCOUNT_REQUESTED' and aggregate_id = ?",
                Integer.class, String.valueOf(ownerId));
    }

    private String lastError(long ownerId) {
        return jdbc.queryForObject(
                "select last_error from outbox_events where event_type = 'BILLING_ACCOUNT_REQUESTED' and aggregate_id = ?",
                String.class, String.valueOf(ownerId));
    }
}
