package com.petclinic.search.index;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.petclinic.events.PetEvent;

/**
 * One pet in the {@code pets} index, denormalized (owner name and phone included) so a search
 * needs no join and no call to pet-service. Field names match {@code elasticsearch/pets-v1.json}
 * ("dynamic": "strict" rejects anything else).
 *
 * @param ownerUserId auth-service user id of the owner; null for walk-in owners. Customers are
 *                    filtered on it, so they only ever find their own pets.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PetDocument(
        long petId,
        String petName,
        String species,
        String breed,
        String allergies,
        long ownerId,
        Long ownerUserId,
        String ownerName,
        String ownerPhone,
        long updatedAt) {

    public static PetDocument from(PetEvent event) {
        return new PetDocument(
                event.getPetId(),
                event.getPetName(),
                event.getSpecies(),
                emptyToNull(event.getBreed()),
                emptyToNull(event.getAllergies()),
                event.getOwnerId(),
                event.getOwnerUserId() > 0 ? event.getOwnerUserId() : null,
                emptyToNull(event.getOwnerName()),
                emptyToNull(event.getOwnerPhone()),
                event.getTimestamp());
    }

    /** proto3 has no null strings: "" means "not set". */
    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
