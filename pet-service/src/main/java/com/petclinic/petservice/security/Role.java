package com.petclinic.petservice.security;

/** Mirrors auth-service's roles; the gateway forwards the role from the verified JWT. */
public enum Role {
    ADMIN,
    VET,
    RECEPTIONIST,
    CUSTOMER;

    /** Clinic employees see every owner; customers only see their own data. */
    public boolean isStaff() {
        return this != CUSTOMER;
    }
}
