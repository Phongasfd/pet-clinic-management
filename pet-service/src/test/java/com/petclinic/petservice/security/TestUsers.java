package com.petclinic.petservice.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/** Puts a gateway-style principal in the security context for service-level tests. */
public final class TestUsers {

    private TestUsers() {
    }

    public static CurrentUser as(Role role, long userId) {
        CurrentUser user = new CurrentUser(userId, role.name().toLowerCase() + userId, role);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
        return user;
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
