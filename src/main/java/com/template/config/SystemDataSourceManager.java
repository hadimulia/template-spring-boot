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

/**
 * Auto-creates and migrates the dedicated system realm database (sims_system)
 * on first use, mirroring {@link SchoolDataSourceManager}. Used by the routing
 * DataSource when the login school code is {@code system}.
 */
@Component
public class SystemDataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(SystemDataSourceManager.class);
    private static final String DB_NAME = "sims_system";

    private final SchoolDataSourceProperties props;
    private volatile HikariDataSource pool;

    public SystemDataSourceManager(SchoolDataSourceProperties props) {
        this.props = props;
    }

    public DataSource getOrCreate() {
        if (pool == null) {
            synchronized (this) {
                if (pool == null) {
                    ensureDatabaseExists();
                    pool = buildPool();
                    runFlyway();
                }
            }
        }
        return pool;
    }

    private void ensureDatabaseExists() {
        try (Connection conn = DriverManager.getConnection(props.getAdminUrl(),
                props.getUsername(), props.getPassword());
             PreparedStatement check = conn.prepareStatement(
                     "SELECT 1 FROM pg_database WHERE datname = ?")) {
            check.setString(1, DB_NAME);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
            try (PreparedStatement create = conn.prepareStatement(
                    "CREATE DATABASE \"" + DB_NAME + "\"")) {
                create.executeUpdate();
                log.info("Created system database {}", DB_NAME);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create system database " + DB_NAME, e);
        }
    }

    private HikariDataSource buildPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getUrlPrefix() + DB_NAME);
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setDriverClassName(props.getDriverClassName());
        config.setPoolName("system-pool");
        config.setMaximumPoolSize(props.getMaxPoolSize());
        return new HikariDataSource(config);
    }

    private void runFlyway() {
        Flyway flyway = Flyway.configure()
                .dataSource(pool)
                .locations("classpath:db/system")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Migrated system database {} (schema version {})",
                DB_NAME, flyway.info().current().getVersion());
    }
}