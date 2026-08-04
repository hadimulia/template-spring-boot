package com.template.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the per-school PostgreSQL databases: auto-creates a school's database
 * ({@code sims_<code>}) on first use, builds a lazily-cached Hikari pool for it,
 * and runs the school Flyway migration set ({@code classpath:db/migration}) once
 * when the database is first created.
 */
@Component
public class SchoolDataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(SchoolDataSourceManager.class);

    /** PostgreSQL db names are lowercased; strip anything that isn't a safe identifier. */
    private static final String DB_NAME_PATTERN = "^[a-z0-9_]+$";

    private final SchoolDataSourceProperties props;
    private final ConcurrentMap<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    public SchoolDataSourceManager(SchoolDataSourceProperties props) {
        this.props = props;
    }

    /**
     * Returns a pooled DataSource for the school database, creating the database
     * and migrating it on first access.
     *
     * @param schoolCode the school code (e.g. {@code DEFAULT}) — used to derive
     *                   the db name {@code sims_<code>}.
     */
    public DataSource getOrCreate(String schoolCode) {
        return getOrCreateByDbName(databaseName(schoolCode));
    }

    /**
     * Same as {@link #getOrCreate(String)} but keyed by the physical db name
     * directly. Used by {@link TenantDataSource}, whose routing key is the db
     * name (e.g. {@code sims_default}), so the name must not be re-prefixed.
     */
    public DataSource getOrCreateByDbName(String dbName) {
        return pools.computeIfAbsent(dbName, this::createSchoolDataSource);
    }

    /** Sanitizes and derives the physical database name for a school code. */
    public String databaseName(String schoolCode) {
        if (schoolCode == null || schoolCode.isBlank()) {
            throw new IllegalArgumentException("School code must not be empty");
        }
        String safe = schoolCode.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");
        return "sims_" + safe;
    }

    /** Creates (if needed), migrates, and pools a school database. */
    private HikariDataSource createSchoolDataSource(String dbName) {
        ensureDatabaseExists(dbName);
        HikariDataSource ds = buildPool(dbName);
        runSchoolFlyway(dbName, ds);
        return ds;
    }

    private void ensureDatabaseExists(String dbName) {
        try (Connection conn = DriverManager.getConnection(props.getAdminUrl(),
                props.getUsername(), props.getPassword());
             PreparedStatement check = conn.prepareStatement(
                     "SELECT 1 FROM pg_database WHERE datname = ?")) {
            check.setString(1, dbName);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
            try (PreparedStatement create = conn.prepareStatement(
                    "CREATE DATABASE \"" + dbName + "\"")) {
                create.executeUpdate();
                log.info("Created school database {}", dbName);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create school database " + dbName, e);
        }
    }

    private HikariDataSource buildPool(String dbName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getUrlPrefix() + dbName);
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setDriverClassName(props.getDriverClassName());
        config.setPoolName("school-" + dbName);
        config.setMaximumPoolSize(props.getMaxPoolSize());
        return new HikariDataSource(config);
    }

    private void runSchoolFlyway(String dbName, DataSource ds) {
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Migrated school database {} (schema version {})", dbName, flyway.info().current().getVersion());
    }

    /** Validates a db name derived from a user-supplied code before any DDL. */
    private boolean isValidDbName(String dbName) {
        return dbName != null && dbName.matches(DB_NAME_PATTERN);
    }
}
