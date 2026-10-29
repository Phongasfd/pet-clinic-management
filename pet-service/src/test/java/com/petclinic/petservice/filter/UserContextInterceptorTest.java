package com.petclinic.petservice.filter;

import com.petclinic.petservice.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class UserContextInterceptorTest {

    private final UserContextInterceptor interceptor = new UserContextInterceptor();

    @AfterEach
    void clear() {
        UserContextHolder.clearContext();
    }

    @Test
    void rejectsRequestsThatDidNotComeThroughTheGateway() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(new MockHttpServletRequest("GET", "/api/v1/owners/1"), response, null);

        assertFalse(proceed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void acceptsGatewayIdentityHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/owners/1");
        request.addHeader("X-User-Id", "12");
        request.addHeader("X-User-Name", "vet1");
        request.addHeader("X-User-Role", "VET");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), null));
        assertEquals("12", UserContextHolder.getContext().getUserId());
    }
}
