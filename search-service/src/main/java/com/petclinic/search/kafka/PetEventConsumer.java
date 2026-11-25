package com.petclinic.search.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.petclinic.events.PetEvent;
import com.petclinic.search.index.PetIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * Keeps the pets index in step with pet-service: every PET_CREATED / PET_UPDATED is a full
 * snapshot of the pet and is written as is (see {@link PetIndexer} for ordering and duplicates).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetEventConsumer {

    static final Set<String> INDEXED_TYPES = Set.of("PET_CREATED", "PET_UPDATED");

    private final PetIndexer petIndexer;

    @KafkaListener(topics = "${app.kafka.topics.pet-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, byte[]> record) {
        PetEvent event = parse(record);
        if (!INDEXED_TYPES.contains(event.getEventType())) {
            log.debug("Ignoring event type {} (not part of the pet document)", event.getEventType());
            return;
        }
        try {
            petIndexer.index(event);
        } catch (IOException e) {
            // Elasticsearch unreachable: let the error handler retry with backoff
            throw new UncheckedIOException("Could not index pet " + event.getPetId(), e);
        }
    }

    static PetEvent parse(ConsumerRecord<String, byte[]> record) {
        try {
            return PetEvent.parseFrom(record.value());
        } catch (InvalidProtocolBufferException | NullPointerException e) {
            throw new InvalidPetEventException("Invalid PetEvent payload, key=" + record.key()
                    + " partition=" + record.partition() + " offset=" + record.offset(), e);
        }
    }
}
