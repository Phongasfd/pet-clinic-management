package com.petclinic.petservice;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Upgrading a database written by the previous release. That release let a pet hold two ACTIVE
 * boarding stays, and a cage two ACTIVE tickets, so V4's partial unique indexes used to fail on
 * such data: Flyway rolled V4 back and pet-service exited on every start.
 */
class V4UpgradeMigrationTest {

    private static EmbeddedPostgres postgres;

    @BeforeAll
    static void startPostgres() throws Exception {
        postgres = EmbeddedPostgres.builder().start();
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        postgres.close();
    }

    @Test
    void duplicateActiveStaysFromTheOldReleaseAreClosedAndTheIndexesAreBuilt() throws Exception {
        DataSource dataSource = postgres.getPostgresDatabase();
        Flyway.configure().dataSource(dataSource).target("3").load().migrate();

        // V2 seeds pets 1-5 and cages 1-5. Reproduce what the old check-in allowed.
        try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
            sql.executeUpdate("UPDATE cages SET status = 'OCCUPIED' WHERE id IN (1, 2, 3)");
            sql.executeUpdate("""
                    INSERT INTO boarding_tickets (id, pet_id, cage_id, check_in_date, expected_check_out_date, status) VALUES
                    (101, 1, 1, '2026-10-01 09:00', '2026-10-05 09:00', 'ACTIVE'),
                    (102, 1, 2, '2026-10-02 09:00', '2026-10-06 09:00', 'ACTIVE'),
                    (103, 2, 3, '2026-10-01 09:00', '2026-10-05 09:00', 'ACTIVE'),
                    (104, 3, 3, '2026-10-03 09:00', '2026-10-07 09:00', 'ACTIVE')
                    """);
        }

        Flyway.configure().dataSource(dataSource).load().migrate();

        try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
            Map<Long, String> tickets = new HashMap<>();
            try (ResultSet rows = sql.executeQuery(
                    "SELECT id, status, actual_check_out_date FROM boarding_tickets WHERE id BETWEEN 101 AND 104")) {
                while (rows.next()) {
                    tickets.put(rows.getLong("id"), rows.getString("status"));
                    if ("COMPLETED".equals(rows.getString("status"))) {
                        assertThat(rows.getTimestamp("actual_check_out_date")).isNotNull();
                    }
                }
            }
            // pet 1 keeps its newest stay (102); cage 3 keeps its newest ticket (104)
            assertThat(tickets).containsExactlyInAnyOrderEntriesOf(Map.of(
                    101L, "COMPLETED", 102L, "ACTIVE", 103L, "COMPLETED", 104L, "ACTIVE"));

            Map<Long, String> cages = new HashMap<>();
            try (ResultSet rows = sql.executeQuery("SELECT id, status FROM cages WHERE id IN (1, 2, 3)")) {
                while (rows.next()) {
                    cages.put(rows.getLong("id"), rows.getString("status"));
                }
            }
            // cage 1 lost its only active stay and is free again
            assertThat(cages).containsExactlyInAnyOrderEntriesOf(Map.of(
                    1L, "AVAILABLE", 2L, "OCCUPIED", 3L, "OCCUPIED"));

            // the indexes exist and hold: a second active ticket for cage 3 is refused
            assertThatThrownBy(() -> sql.executeUpdate("""
                    INSERT INTO boarding_tickets (pet_id, cage_id, check_in_date, expected_check_out_date, status)
                    VALUES (4, 3, NOW(), NOW(), 'ACTIVE')
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uq_boarding_active_cage");
        }
    }
}
