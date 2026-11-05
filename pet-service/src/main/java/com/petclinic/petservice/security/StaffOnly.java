package com.petclinic.petservice.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Any clinic employee: front desk (boarding, spa, owner search) and clinical work. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'VET', 'RECEPTIONIST')")
public @interface StaffOnly {
}
