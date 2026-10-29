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
        LocalDateTime now = LocalDateTime.now(clock);
        PetEvent event = PetEvent.newBuilder()
                // generated once here, so redeliveries carry the same id and consumers can dedupe
                .setEventId(UUID.randomUUID().toString())
                .setEventType(OutboxEvent.PET_CREATED)
                .setTimestamp(now.atZone(clock.getZone()).toInstant().toEpochMilli())
                .setPetId(pet.getId())
                .setPetName(pet.getName())
                .setSpecies(pet.getSpecies().name())
                .setBreed(pet.getBreed() != null ? pet.getBreed() : "") // breed có thể null
                .setOwnerId(pet.getOwner().getId())
                .build();

        outboxEventRepository.save(OutboxEvent.of(
                OutboxEvent.PET_CREATED, String.valueOf(pet.getId()), event.toByteArray(), now));
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
