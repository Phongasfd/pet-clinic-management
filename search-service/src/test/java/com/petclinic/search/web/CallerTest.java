package com.petclinic.search.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallerTest {

    @Test
    void parsesGatewayHeaders() {
        Caller caller = Caller.fromHeaders("42", "CUSTOMER");

        assertEquals(42L, caller.userId());
        assertFalse(caller.isStaff());
        assertTrue(Caller.fromHeaders("1", "RECEPTIONIST").isStaff());
    }

    @Test
    void missingHeadersAreUnauthenticatedNotAdmin() {
        assertThrows(UnauthenticatedException.class, () -> Caller.fromHeaders(null, "ADMIN"));
        assertThrows(UnauthenticatedException.class, () -> Caller.fromHeaders("42", null));
        assertThrows(UnauthenticatedException.class, () -> Caller.fromHeaders(" ", " "));
    }

    @Test
    void malformedHeadersAreRejected() {
        assertThrows(UnauthenticatedException.class, () -> Caller.fromHeaders("abc", "VET"));
        assertThrows(UnauthenticatedException.class, () -> Caller.fromHeaders("42", "ROOT"));
    }
}
