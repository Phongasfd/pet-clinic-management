package com.petclinic.petservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.petclinic.petservice.security.Role;
import com.petclinic.petservice.service.BillingClientService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application against a real PostgreSQL (embedded binaries, Flyway V1-V4 applied, so row
 * locks and partial unique indexes behave as in production) and an in-JVM Kafka broker.
 * No Docker needed. One shared Spring context for every subclass: keep the config identical.
 *
 * <p>billing-service is the only stub (it is another process); the outbox relay runs for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.outbox.poll-interval-ms=200",
        "spring.jpa.show-sql=false"
})
public abstract class AbstractIntegrationTest {

    /** One PostgreSQL process for the whole test JVM; zonky stops it on JVM exit. */
    private static final EmbeddedPostgres POSTGRES = startPostgres();

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @MockBean
    protected BillingClientService billingClientService;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Tests share one database, so every test makes its own users and phone numbers. */
    protected static long newUserId() {
        return 1_000_000 + SEQUENCE.incrementAndGet();
    }

    protected static String newPhone() {
        return String.format("08%08d", SEQUENCE.incrementAndGet());
    }

    /** The identity headers the api-gateway injects after verifying a JWT. */
    protected static RequestPostProcessor as(Role role, long userId) {
        return request -> {
            request.addHeader("X-User-Id", String.valueOf(userId));
            request.addHeader("X-User-Name", role.name().toLowerCase() + userId);
            request.addHeader("X-User-Role", role.name());
            return request;
        };
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected static long idOf(MvcResult result) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    /** Owner profile for a customer account, created the way the customer would. */
    protected long createOwnerAs(Role role, long userId, String fullName) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/owners").with(as(role, userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", fullName, "phoneNumber", newPhone()))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    protected long addPetAs(Role role, long userId, long ownerId) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/pets").with(as(role, userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Milo", "species", "DOG", "ownerId", ownerId))))
                .andExpect(status().isCreated())
                .andReturn());
    }
}
