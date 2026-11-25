package com.petclinic.petservice.outbox;

import com.petclinic.billing.grpc.BillingRequest;
import com.petclinic.events.PetEvent;
import com.petclinic.petservice.entity.OutboxEvent;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records side effects in the outbox table instead of performing them inline.
 *
 * <p>Must run inside the caller's transaction ({@link Propagation#MANDATORY}): the whole
 * point is that the outbox row commits or rolls back together with the business data.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    public void enqueuePetCreated(Pet pet) {
        enqueuePetEvent(OutboxEvent.PET_CREATED, pet);
    }

    /**
     * The pet must already be flushed, so {@code pet.getVersion()} is the version of this change
     * (Hibernate bumps @Version on flush, not on the setter).
     */
    public void enqueuePetUpdated(Pet pet) {
        enqueuePetEvent(OutboxEvent.PET_UPDATED, pet);
    }

    private void enqueuePetEvent(String eventType, Pet pet) {
        LocalDateTime now = LocalDateTime.now(clock);
        Owner owner = pet.getOwner();
        PetEvent event = PetEvent.newBuilder()
                // generated once here, so redeliveries carry the same id and consumers can dedupe
                .setEventId(UUID.randomUUID().toString())
                .setEventType(eventType)
                .setTimestamp(now.atZone(clock.getZone()).toInstant().toEpochMilli())
                .setPetId(pet.getId())
                .setPetName(pet.getName())
                .setSpecies(pet.getSpecies().name())
                .setBreed(nullToEmpty(pet.getBreed()))
                .setOwnerId(owner.getId())
                // full snapshot (see pet_events.proto): search-service indexes the event as is
                .setOwnerUserId(owner.getUserId() != null ? owner.getUserId() : 0L)
                .setOwnerName(nullToEmpty(owner.getFullName()))
                .setOwnerPhone(nullToEmpty(owner.getPhoneNumber()))
                .setAllergies(nullToEmpty(pet.getAllergies()))
                .setVersion(pet.getVersion() != null ? pet.getVersion() : 0L)
                .build();

        outboxEventRepository.save(OutboxEvent.of(
                eventType, String.valueOf(pet.getId()), event.toByteArray(), now));
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : ""; // proto3 strings can't be null
    }

    public void enqueueBillingAccount(Owner owner) {
        BillingRequest.Builder request = BillingRequest.newBuilder()
                .setCustomerId(String.valueOf(owner.getId()))
                .setCustomerName(owner.getFullName());
        if (owner.getEmail() != null) {
            request.setEmail(owner.getEmail());
        }

        outboxEventRepository.save(OutboxEvent.of(
                OutboxEvent.BILLING_ACCOUNT_REQUESTED, String.valueOf(owner.getId()),
                request.build().toByteArray(), LocalDateTime.now(clock)));
    }
}
