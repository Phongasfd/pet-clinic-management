package com.petclinic.search.web;

import com.petclinic.search.search.PetSearchResponse;
import com.petclinic.search.search.PetSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final PetSearchService petSearchService;

    /**
     * {@code GET /api/v1/search/pets?q=milu&species=DOG&page=0&size=20}. Staff search every pet,
     * customers only their own. Results are eventually consistent: a pet shows up here about a
     * second after pet-service commits it (outbox poll + Kafka + index refresh).
     */
    @GetMapping("/pets")
    public PetSearchResponse searchPets(
            @RequestHeader(value = Caller.HEADER_USER_ID, required = false) String userId,
            @RequestHeader(value = Caller.HEADER_USER_ROLE, required = false) String role,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String species,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws IOException {
        return petSearchService.search(Caller.fromHeaders(userId, role), q, species, page, size);
    }
}
