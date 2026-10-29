package com.petclinic.apigateway.filter;

import com.petclinic.apigateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationFilterTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaHMyNTY=";

    private GatewayFilter filter;
    private final AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange.getRequest());
        return Mono.empty();
    };

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        filter = new AuthenticationFilter(new RouteValidator(), jwtUtil).apply(new AuthenticationFilter.Config());
        forwarded.set(null);
    }

    private String token(Object userId, String role) {
        var builder = Jwts.builder()
                .subject("alice")
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)));
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.compact();
    }

    @Test
    void forwardsIdentityFromTheTokenAndIgnoresSpoofedHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/owners/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(12, "CUSTOMER"))
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN"));

        filter.filter(exchange, chain).block();

        HttpHeaders headers = forwarded.get().getHeaders();
        assertEquals("12", headers.getFirst("X-User-Id"));
        assertEquals("CUSTOMER", headers.getFirst("X-User-Role"));
        assertEquals(1, headers.get("X-User-Role").size());
    }

    @Test
    void stripsSpoofedHeadersOnOpenEndpointsToo() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login")
                .header("X-User-Role", "ADMIN"));

        filter.filter(exchange, chain).block();

        assertNull(forwarded.get().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void rejectsMissingToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/pets/1"));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @Test
    void pathThatOnlyContainsAnOpenPrefixIsStillSecured() {
        // the old contains() check let this through without a token
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/owners/swagger-ui"));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsTokenWithoutUserId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/pets/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(null, "VET")));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
