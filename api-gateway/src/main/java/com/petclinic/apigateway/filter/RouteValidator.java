package com.petclinic.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // Exact paths only. The old check used contains(), so any URL that merely
    // contained e.g. "/swagger-ui" (/api/v1/owners/swagger-ui) skipped JWT validation.
    public static final Set<String> openApiEndpoints = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> !openApiEndpoints.contains(normalize(request.getURI().getPath()));

    private static String normalize(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
