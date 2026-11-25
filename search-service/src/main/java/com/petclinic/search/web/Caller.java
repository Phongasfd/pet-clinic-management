package com.petclinic.search.web;

/**
 * The caller, as identified by the api-gateway. The gateway verifies the JWT, strips any
 * client-sent {@code X-User-*} headers and injects its own; search-service has no public port,
 * so the headers can be trusted. Missing or malformed headers mean 401, never "see everything".
 */
public record Caller(long userId, Role role) {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    public boolean isStaff() {
        return role.isStaff();
    }

    public static Caller fromHeaders(String userId, String role) {
        if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
            throw new UnauthenticatedException("Missing gateway identity headers");
        }
        try {
            return new Caller(Long.parseLong(userId.trim()), Role.valueOf(role.trim()));
        } catch (IllegalArgumentException e) { // non-numeric id or unknown role
            throw new UnauthenticatedException("Invalid gateway identity headers");
        }
    }
}
