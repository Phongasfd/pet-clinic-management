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

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

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
            ServerHttpRequest request = exchange.getRequest();

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
                    // If you have userId in claims, you can extract it similarly:
                    // String userId = claims.get("id", String.class);

                    request = exchange.getRequest()
                            .mutate()
                            .header("X-User-Name", username)
                            .header("X-User-Role", role)
                            // .header("X-User-Id", userId) 
                            .build();

                } catch (Exception e) {
                    return onError(exchange, "Unauthorized access to application: " + e.getMessage());
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
