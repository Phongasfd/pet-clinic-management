package com.petclinic.search.search;

import java.util.List;
import java.util.Map;

/**
 * @param speciesCounts matches per species for the same query (facets), ignoring paging
 */
public record PetSearchResponse(long total, int page, int size, List<PetHit> items, Map<String, Long> speciesCounts) {

    public record PetHit(
            long petId,
            String petName,
            String species,
            String breed,
            String allergies,
            long ownerId,
            String ownerName,
            String ownerPhone,
            Double score) {
    }
}
