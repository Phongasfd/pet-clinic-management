package com.petclinic.petservice.security;

/**
 * The caller, as identified by the gateway. {@code userId} is auth-service's user id;
 * owners created by a customer are linked to it ({@code owners.user_id}).
 */
public record CurrentUser(Long userId, String username, Role role) {

    public boolean isStaff() {
        return role.isStaff();
    }
}
