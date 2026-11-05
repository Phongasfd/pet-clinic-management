package com.petclinic.petservice.security;

import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.entity.Pet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class AccessGuardTest {

    private final AccessGuard guard = new AccessGuard();

    private final Owner alicesProfile = Owner.builder().id(1L).userId(100L).build();
    private final Owner walkInOwner = Owner.builder().id(2L).userId(null).build();

    @AfterEach
    void clear() {
        TestUsers.clear();
    }

    @Test
    void customerCanAccessOwnProfileAndPets() {
        TestUsers.as(Role.CUSTOMER, 100L);

        assertDoesNotThrow(() -> guard.checkOwnerAccess(alicesProfile));
        assertDoesNotThrow(() -> guard.checkPetAccess(Pet.builder().owner(alicesProfile).build()));
    }

    @Test
    void customerCannotAccessSomeoneElsesData() {
        TestUsers.as(Role.CUSTOMER, 200L);

        assertThrows(AccessDeniedException.class, () -> guard.checkOwnerAccess(alicesProfile));
        assertThrows(AccessDeniedException.class, () -> guard.checkPetAccess(Pet.builder().owner(alicesProfile).build()));
        // a walk-in owner has no account, so no customer owns it
        assertThrows(AccessDeniedException.class, () -> guard.checkOwnerAccess(walkInOwner));
    }

    @Test
    void staffCanAccessEveryOwner() {
        for (Role staff : new Role[]{Role.ADMIN, Role.VET, Role.RECEPTIONIST}) {
            TestUsers.as(staff, 1L);
            assertDoesNotThrow(() -> guard.checkOwnerAccess(alicesProfile));
            assertDoesNotThrow(() -> guard.checkOwnerAccess(walkInOwner));
        }
    }

    @Test
    void noPrincipalMeansNotAuthenticated() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, guard::currentUser);
    }
}
