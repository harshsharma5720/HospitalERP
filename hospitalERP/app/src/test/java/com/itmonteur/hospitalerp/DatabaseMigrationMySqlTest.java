package com.itmonteur.hospitalerp;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Flyway migrations on a real MySQL 8 (throw-away Docker container; skipped when Docker isn't there).
 *
 * 1. Upgrade: a database like the ones the old ddl-auto=update built (V1 structure incl. the columns
 *    removed in step 1.3, some data, no Flyway history). The app starts on it: Flyway baselines it at
 *    version 1 and runs the later migrations, Hibernate validates the entities against it.
 * 2. Fresh install: all migrations on an empty database give exactly the same schema.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "REMINDERS_ENABLED=false")
class DatabaseMigrationMySqlTest {

    private static final String[] LEGACY_COLUMNS = {
            "doctor.password", "doctor.role", "patient.role", "patient_relative.role", "receptionist.role"};

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("hospital")
            .withCommand("--innodb-buffer-pool-size=32M", "--performance-schema=OFF"); // small footprint

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws SQLException {
        createExistingDatabase();
        registry.add("DB_URL", MYSQL::getJdbcUrl);
        registry.add("DB_USERNAME", MYSQL::getUsername);
        registry.add("DB_PASSWORD", MYSQL::getPassword);
    }

    /** What a database built by the old ddl-auto=update looks like: V1 structure and data, no Flyway history. */
    private static void createExistingDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__baseline.sql"));
            var statement = connection.createStatement();
            statement.execute("INSERT INTO users (id, username, password, email, phone_number, role) VALUES "
                    + "(1, 'rao', 'x', 'rao@example.com', '+911111111111', 'DOCTOR'), "
                    + "(2, 'asha', 'x', 'asha@example.com', '+912222222222', 'PATIENT')");
            statement.execute("INSERT INTO doctor (id, user_id, name, email, phone_number, specialist, password, role) "
                    + "VALUES (1, 1, 'Rao', 'rao@example.com', '+911111111111', 'CARDIOLOGY', 'old-hash', 'DOCTOR')");
            statement.execute("INSERT INTO patient (patient_id, user_id, patient_name, email, role) "
                    + "VALUES (1, 2, 'Asha', 'asha@example.com', 'PATIENT')");
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void existingDatabaseIsBaselinedAndCleanedUpWithoutLosingData() {
        // The context started, so Hibernate's ddl-auto=validate accepted the migrated schema
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, type, success FROM flyway_schema_history ORDER BY installed_rank");
        assertThat(history).extracting(row -> row.get("version") + " " + row.get("type"))
                .containsExactly("1 BASELINE", "2026.09.28.1 SQL", "2026.09.28.2 SQL");
        assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isIn(true, 1));

        assertThat(legacyColumnsIn(jdbc, "hospital")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT name FROM doctor WHERE id = 1", String.class)).isEqualTo("Rao");
        assertThat(jdbc.queryForObject("SELECT patient_name FROM patient WHERE patient_id = 1", String.class)).isEqualTo("Asha");
    }

    @Test
    void freshInstallGivesExactlyTheUpgradedSchema() throws SQLException {
        try (Connection root = DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword())) {
            root.createStatement().execute("CREATE DATABASE IF NOT EXISTS fresh");
        }
        String freshUrl = MYSQL.getJdbcUrl().replace("/hospital", "/fresh");
        Flyway.configure()
                .dataSource(freshUrl, "root", MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate rootJdbc = new JdbcTemplate(new DriverManagerDataSource(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword()));
        assertThat(describe(rootJdbc, "fresh")).isEqualTo(describe(rootJdbc, "hospital"));
        assertThat(legacyColumnsIn(rootJdbc, "fresh")).isEmpty();
    }

    /** Tables, columns (type, null, default, key, extra), indexes and foreign keys, without Flyway's own table. */
    private static List<String> describe(JdbcTemplate jdbc, String schema) {
        List<String> lines = new ArrayList<>();
        lines.addAll(jdbc.queryForList("""
                SELECT CONCAT_WS(' | ', table_name, column_name, column_type, is_nullable,
                                 IFNULL(column_default, 'NULL'), column_key, extra)
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name <> 'flyway_schema_history'
                ORDER BY table_name, column_name""", String.class, schema));
        lines.addAll(jdbc.queryForList("""
                SELECT CONCAT_WS(' | ', 'index', table_name, index_name, non_unique, seq_in_index, column_name)
                FROM information_schema.statistics
                WHERE table_schema = ? AND table_name <> 'flyway_schema_history'
                ORDER BY table_name, index_name, seq_in_index""", String.class, schema));
        lines.addAll(jdbc.queryForList("""
                SELECT CONCAT_WS(' | ', 'fk', k.table_name, k.constraint_name, k.column_name,
                                 k.referenced_table_name, k.referenced_column_name, r.delete_rule)
                FROM information_schema.key_column_usage k
                JOIN information_schema.referential_constraints r
                  ON r.constraint_schema = k.constraint_schema AND r.constraint_name = k.constraint_name
                WHERE k.table_schema = ?
                ORDER BY k.table_name, k.constraint_name""", String.class, schema));
        return lines;
    }

    private static List<String> legacyColumnsIn(JdbcTemplate jdbc, String schema) {
        return jdbc.queryForList("""
                SELECT CONCAT(table_name, '.', column_name) FROM information_schema.columns
                WHERE table_schema = ? AND CONCAT(table_name, '.', column_name) IN (?, ?, ?, ?, ?)""",
                String.class, schema, LEGACY_COLUMNS[0], LEGACY_COLUMNS[1], LEGACY_COLUMNS[2],
                LEGACY_COLUMNS[3], LEGACY_COLUMNS[4]);
    }
}
