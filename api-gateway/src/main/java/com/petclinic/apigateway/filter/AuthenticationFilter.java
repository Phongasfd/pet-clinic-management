package com.petclinic.apigateway.filter;

import com.petclinic.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    private static final List<String> IDENTITY_HEADERS = List.of(HEADER_USER_ID, HEADER_USER_NAME, HEADER_USER_ROLE);

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            // Identity headers are only ever set by this filter. Drop whatever the client sent,
            // otherwise anyone could call a service as "X-User-Id: 1" / "X-User-Role: ADMIN".
            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
                    .build();

            if (validator.isSecured.test(request)) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Missing authorization header");
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                } else {
                    return onError(exchange, "Invalid authorization header format");
                }

                try {
                    Claims claims = jwtUtil.validateTokenAndGetClaims(authHeader);
                    
                    // Add headers
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);
                    Object userId = claims.get("userId");
                    if (userId == null || role == null) {
                        // tokens issued before userId was added: force a re-login
                        return onError(exchange, "Token is missing required claims, please log in again");
                    }

                    request = request
                            .mutate()
                            .header(HEADER_USER_ID, String.valueOf(userId))
                            .header(HEADER_USER_NAME, username)
                            .header(HEADER_USER_ROLE, role)
                            .build();

                } catch (Exception e) {
                    // don't echo parser details back to the caller
                    return onError(exchange, "Invalid or expired token");
                }
            }
            return chain.filter(exchange.mutate().request(request).build());
        });
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange, String err) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String body = "{\"error\": \"Unauthorized\", \"message\": \"" + err + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // configuration properties
    }
}
