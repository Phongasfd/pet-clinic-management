package com.petclinic.search.kafka;

import com.petclinic.events.PetEvent;
import com.petclinic.search.index.PetIndexer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PetEventConsumerTest {

    private PetIndexer indexer;
    private PetEventConsumer consumer;

    @BeforeEach
    void setUp() {
        indexer = mock(PetIndexer.class);
        consumer = new PetEventConsumer(indexer);
    }

    private static ConsumerRecord<String, byte[]> record(byte[] value) {
        return new ConsumerRecord<>("pet-events", 0, 0L, "42", value);
    }

    private static byte[] event(String type) {
        return PetEvent.newBuilder().setEventId("e1").setEventType(type).setPetId(42)
                .setPetName("Milu").setSpecies("DOG").setOwnerId(7).build().toByteArray();
    }

    @Test
    void indexesCreatedAndUpdatedEvents() throws Exception {
        consumer.consume(record(event("PET_CREATED")));
        consumer.consume(record(event("PET_UPDATED")));

        verify(indexer, times(2)).index(any());
    }

    @Test
    void ignoresOtherEventTypes() throws Exception {
        consumer.consume(record(event("APPOINTMENT_COMPLETED")));

        verify(indexer, never()).index(any());
    }

    @Test
    void corruptPayloadIsNotRetryable() {
        byte[] truncated = {0x0A, 0x05, 'a'}; // field 1 says 5 bytes, only 1 follows

        assertThrows(InvalidPetEventException.class, () -> consumer.consume(record(truncated)));
        assertThrows(InvalidPetEventException.class, () -> consumer.consume(record(null)));
    }

    @Test
    void elasticsearchOutageIsRethrownForRetry() throws Exception {
        when(indexer.index(any())).thenThrow(new IOException("Connection refused"));

        assertThrows(UncheckedIOException.class, () -> consumer.consume(record(event("PET_CREATED"))));
    }
}
