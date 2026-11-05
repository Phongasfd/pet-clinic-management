package com.petclinic.petservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GatewayHeaderAuthenticationFilterTest {

    private final GatewayHeaderAuthenticationFilter filter = new GatewayHeaderAuthenticationFilter();

    private Authentication authenticationSeenDownstream(MockHttpServletRequest request) throws Exception {
        AtomicReference<Authentication> seen = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) {
                seen.set(SecurityContextHolder.getContext().getAuthentication());
            }
        });
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return seen.get();
    }

    private static MockHttpServletRequest request(String userId, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/owners/1");
        if (userId != null) request.addHeader("X-User-Id", userId);
        if (role != null) request.addHeader("X-User-Role", role);
        request.addHeader("X-User-Name", "someone");
        return request;
    }

    @Test
    void gatewayHeadersBecomeAPrincipalWithARole() throws Exception {
        Authentication auth = authenticationSeenDownstream(request("12", "VET"));

        CurrentUser user = (CurrentUser) auth.getPrincipal();
        assertEquals(12L, user.userId());
        assertEquals(Role.VET, user.role());
        assertEquals("ROLE_VET", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElseThrow());
    }

    @Test
    void requestsWithoutGatewayHeadersStayAnonymous() throws Exception {
        assertNull(authenticationSeenDownstream(request(null, null)));
        assertNull(authenticationSeenDownstream(request("12", null)));
    }

    @Test
    void malformedHeadersAreRejectedNotGuessed() throws Exception {
        assertNull(authenticationSeenDownstream(request("abc", "VET")));
        assertNull(authenticationSeenDownstream(request("12", "SUPERUSER")));
    }

    @Test
    void contextIsClearedAfterTheRequest() throws Exception {
        authenticationSeenDownstream(request("12", "ADMIN"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
