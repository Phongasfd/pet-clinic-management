package com.petclinic.search.web;

/** Mirrors auth-service's roles; the gateway forwards the role from the verified JWT. */
public enum Role {
    ADMIN,
    VET,
    RECEPTIONIST,
    CUSTOMER;

    /** Clinic employees search every pet; customers only their own. */
    public boolean isStaff() {
        return this != CUSTOMER;
    }
}
