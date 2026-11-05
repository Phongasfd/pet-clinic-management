package com.petclinic.petservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Turns the identity headers set by the api-gateway into a Spring Security principal.
 *
 * <p>The gateway verifies the JWT, strips any client-sent {@code X-User-*} headers and injects
 * its own, and pet-service has no public port, so these headers can be trusted here.
 * Requests without them (or with an unknown role) stay anonymous and get 401.
 *
 * <p>Not a {@code @Component}: Spring Boot would also register it as a plain servlet filter,
 * so it would run twice. {@link SecurityConfig} adds it to the security chain.
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CurrentUser user = parse(request);
        if (user != null) {
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    static CurrentUser parse(HttpServletRequest request) {
        String userId = request.getHeader(HEADER_USER_ID);
        String role = request.getHeader(HEADER_USER_ROLE);
        if (isBlank(userId) || isBlank(role)) {
            return null;
        }
        try {
            return new CurrentUser(Long.valueOf(userId.trim()), request.getHeader(HEADER_USER_NAME),
                    Role.valueOf(role.trim()));
        } catch (IllegalArgumentException e) { // non-numeric id or unknown role
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
