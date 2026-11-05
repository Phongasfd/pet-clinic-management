package com.petclinic.petservice.security;

import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.entity.Pet;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Ownership checks (object-level authorization). A role check alone is not enough:
 * every customer has the CUSTOMER role, so without this any customer could read or
 * change another customer's pets just by changing an id in the URL (IDOR).
 */
@Component
public class AccessGuard {

    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        return user;
    }

    /** Staff may access any owner; a customer only the owner profile linked to their account. */
    public void checkOwnerAccess(Owner owner) {
        CurrentUser user = currentUser();
        if (user.isStaff()) {
            return;
        }
        if (owner.getUserId() == null || !owner.getUserId().equals(user.userId())) {
            throw new AccessDeniedException("You can only access your own owner profile and pets");
        }
    }

    public void checkPetAccess(Pet pet) {
        checkOwnerAccess(pet.getOwner());
    }
}
