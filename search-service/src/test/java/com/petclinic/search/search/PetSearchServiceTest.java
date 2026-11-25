package com.petclinic.search.search;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.petclinic.search.web.Caller;
import com.petclinic.search.web.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PetSearchServiceTest {

    private static final Caller CUSTOMER = new Caller(1007, Role.CUSTOMER);
    private static final Caller VET = new Caller(5, Role.VET);

    private static String json(SearchRequest request) {
        return JsonpUtils.toJsonString(request, new JacksonJsonpMapper());
    }

    @Test
    void customerSearchIsAlwaysFilteredToTheirOwnPets() {
        String body = json(PetSearchService.buildRequest("pets", CUSTOMER, "milu", null, 0, 20));

        assertTrue(body.contains("\"ownerUserId\""), body);
        assertTrue(body.contains("1007"), body);
    }

    @Test
    void customerWithoutQueryStillGetsOnlyTheirPets() {
        String body = json(PetSearchService.buildRequest("pets", CUSTOMER, null, null, 0, 20));

        assertTrue(body.contains("\"ownerUserId\""), "an empty query must not mean 'all pets'");
    }

    @Test
    void staffSearchIsNotFilteredByOwner() {
        String body = json(PetSearchService.buildRequest("pets", VET, "milu", "dog", 0, 20));

        assertFalse(body.contains("ownerUserId"), body);
        assertTrue(body.contains("\"DOG\""), "species is normalized to the enum value: " + body);
        assertTrue(body.contains("\"fuzziness\":\"AUTO\""), body);
        assertTrue(body.contains("\"bool_prefix\""), body);
    }

    @Test
    void digitsAlsoSearchTheOwnerPhone() {
        assertTrue(json(PetSearchService.buildRequest("pets", VET, "0912 345", null, 0, 20)).contains("ownerPhone"));
        assertFalse(json(PetSearchService.buildRequest("pets", VET, "milu", null, 0, 20)).contains("ownerPhone"));
    }

    @Test
    void pagingIsTranslatedToFromAndSize() {
        SearchRequest request = PetSearchService.buildRequest("pets", VET, "milu", null, 2, 10);

        assertEquals(20, request.from());
        assertEquals(10, request.size());
    }

    @Test
    void rejectsInvalidPaging() {
        assertThrows(InvalidSearchRequestException.class,
                () -> PetSearchService.buildRequest("pets", VET, "x", null, -1, 20));
        assertThrows(InvalidSearchRequestException.class,
                () -> PetSearchService.buildRequest("pets", VET, "x", null, 0, 51));
        assertThrows(InvalidSearchRequestException.class,
                () -> PetSearchService.buildRequest("pets", VET, "x", null, 500, 50), "beyond max_result_window");
    }

    @Test
    void rejectsOversizedQueryAndInvalidSpecies() {
        assertThrows(InvalidSearchRequestException.class,
                () -> PetSearchService.buildRequest("pets", VET, "x".repeat(101), null, 0, 20));
        assertThrows(InvalidSearchRequestException.class,
                () -> PetSearchService.buildRequest("pets", VET, "x", "DOG\"}", 0, 20));
    }
}
