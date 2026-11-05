package com.petclinic.petservice;

import com.petclinic.petservice.dto.AppointmentDto;
import com.petclinic.petservice.entity.Appointment;
import com.petclinic.petservice.exception.BusinessConflictException;
import com.petclinic.petservice.security.TestUsers;
import com.petclinic.petservice.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.petclinic.petservice.security.Role.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentBookingIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDateTime T = LocalDateTime.of(2035, 3, 1, 9, 0);

    @Autowired private AppointmentService appointmentService;

    @Test
    void petCannotBeDoubleBookedWithinASlot() throws Exception {
        long user = newUserId();
        long petId = addPetAs(CUSTOMER, user, createOwnerAs(CUSTOMER, user, "Eve"));

        long first = idOf(book(user, petId, T).andExpect(status().isCreated()).andReturn());
        book(user, petId, T.plusMinutes(10)).andExpect(status().isConflict());
        book(user, petId, T.minusMinutes(29)).andExpect(status().isConflict());
        book(user, petId, T.plusMinutes(30)).andExpect(status().isCreated()); // the next slot is free

        // a cancelled appointment frees its slot
        cancel(user, first).andExpect(status().isOk());
        book(user, petId, T.minusMinutes(15)).andExpect(status().isCreated());
    }

    @Test
    void lifecycleFollowsTheStateMachine() throws Exception {
        long user = newUserId();
        long petId = addPetAs(CUSTOMER, user, createOwnerAs(CUSTOMER, user, "Frank"));
        long id = idOf(book(user, petId, T).andExpect(status().isCreated()).andReturn());

        changeStatus(CUSTOMER, user, id, "CONFIRMED").andExpect(status().isForbidden());
        changeStatus(RECEPTIONIST, 1, id, "COMPLETED").andExpect(status().isConflict()); // not confirmed yet
        changeStatus(RECEPTIONIST, 1, id, "CONFIRMED").andExpect(status().isOk());
        changeStatus(VET, 2, id, "COMPLETED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        changeStatus(VET, 2, id, "PENDING").andExpect(status().isConflict());   // can't reopen
        cancel(user, id).andExpect(status().isConflict());
    }

    @Test
    void concurrentBookingsForTheSameSlot_onlyOneSucceeds() throws Exception {
        long user = newUserId();
        long petId = addPetAs(CUSTOMER, user, createOwnerAs(CUSTOMER, user, "Grace"));
        AppointmentDto.Request request = AppointmentDto.Request.builder()
                .petId(petId).serviceType(Appointment.ServiceType.VET).appointmentDate(T).build();

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    TestUsers.as(CUSTOMER, user); // security context is per thread
                    go.await();
                    try {
                        appointmentService.bookAppointment(request);
                        return true;
                    } catch (BusinessConflictException e) {
                        return false;
                    } finally {
                        TestUsers.clear();
                    }
                }));
            }
            go.countDown();
            long successes = 0;
            for (Future<Boolean> f : futures) {
                if (f.get(60, TimeUnit.SECONDS)) successes++;
            }
            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
        }
    }

    private ResultActions book(long user, long petId, LocalDateTime at) throws Exception {
        return mockMvc.perform(post("/api/v1/appointments").with(as(CUSTOMER, user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("petId", petId, "serviceType", "VET", "appointmentDate", at.toString()))));
    }

    private ResultActions cancel(long user, long id) throws Exception {
        return changeStatus(CUSTOMER, user, id, "CANCELLED");
    }

    private ResultActions changeStatus(com.petclinic.petservice.security.Role role, long user, long id, String status)
            throws Exception {
        return mockMvc.perform(patch("/api/v1/appointments/{id}/status", id).with(as(role, user))
                .param("newStatus", status));
    }
}
