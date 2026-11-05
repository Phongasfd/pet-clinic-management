package com.petclinic.petservice;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static com.petclinic.petservice.security.Role.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Role rules and ownership (IDOR) rules, through the real HTTP + security stack. */
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void requestsThatDidNotComeThroughTheGatewayGet401() throws Exception {
        mockMvc.perform(get("/api/v1/owners/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/owners/1").header("X-User-Id", "1").header("X-User-Role", "GOD"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerSeesOnlyTheirOwnOwnerProfileAndPets() throws Exception {
        long alice = newUserId();
        long bob = newUserId();
        long aliceOwnerId = createOwnerAs(CUSTOMER, alice, "Alice");
        long petId = addPetAs(CUSTOMER, alice, aliceOwnerId);

        // flat DTOs: returning the Owner/Pet entities serialized owner.pets[].owner... recursively
        mockMvc.perform(get("/api/v1/pets/owner/{ownerId}", aliceOwnerId).with(as(CUSTOMER, alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(petId))
                .andExpect(jsonPath("$[0].ownerId").value(aliceOwnerId));
        mockMvc.perform(get("/api/v1/owners/me").with(as(CUSTOMER, alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceOwnerId))
                .andExpect(jsonPath("$.pets").doesNotExist());

        // Bob changes the id in the URL: denied
        mockMvc.perform(get("/api/v1/pets/owner/{ownerId}", aliceOwnerId).with(as(CUSTOMER, bob)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/owners/{id}", aliceOwnerId).with(as(CUSTOMER, bob)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/pets").with(as(CUSTOMER, bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Intruder", "species", "CAT", "ownerId", aliceOwnerId))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/appointments").with(as(CUSTOMER, bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("petId", petId, "serviceType", "VET",
                                "appointmentDate", "2035-01-10T09:00:00"))))
                .andExpect(status().isForbidden());

        // staff see every owner
        mockMvc.perform(get("/api/v1/owners/{id}", aliceOwnerId).with(as(RECEPTIONIST, 1)))
                .andExpect(status().isOk());
    }

    @Test
    void customerHasOneProfileLinkedToTheirAccount() throws Exception {
        long carol = newUserId();
        createOwnerAs(CUSTOMER, carol, "Carol");

        mockMvc.perform(post("/api/v1/owners").with(as(CUSTOMER, carol))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Carol again", "phoneNumber", newPhone()))))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/owners/me").with(as(CUSTOMER, newUserId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void roleRulesPerEndpoint() throws Exception {
        long dave = newUserId();
        long ownerId = createOwnerAs(CUSTOMER, dave, "Dave");
        long petId = addPetAs(CUSTOMER, dave, ownerId);

        // medical records: vets and admins only, not even the front desk
        for (var role : new com.petclinic.petservice.security.Role[]{CUSTOMER, RECEPTIONIST}) {
            mockMvc.perform(post("/api/v1/pets/{petId}/medical-records", petId).with(as(role, dave))
                            .param("reason", "Checkup"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/pets/{petId}/medical-records", petId).with(as(VET, 2))
                        .param("reason", "Checkup"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(petId));

        // front-desk operations
        mockMvc.perform(patch("/api/v1/pets/{id}/health-info", petId).with(as(CUSTOMER, dave)).param("weight", "5"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/boarding/tickets/{id}/check-out", 1).with(as(CUSTOMER, dave)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/spa-tickets").with(as(CUSTOMER, dave))
                        .param("petId", String.valueOf(petId)).param("services", "Bath"))
                .andExpect(status().isForbidden());

        // phone search would let a customer enumerate other customers
        mockMvc.perform(get("/api/v1/owners").with(as(CUSTOMER, dave)).param("phone", "08"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/owners").with(as(RECEPTIONIST, 3)).param("phone", "08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.totalElements").isNumber());
    }
}
