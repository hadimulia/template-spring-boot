package com.template;

import com.template.config.SystemDataSourceManager;
import com.template.security.CustomUserDetailsService;
import com.template.tenant.TenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: verifies the Spring context boots with the database-per-tenant
 * wiring. Requires a reachable PostgreSQL (the registry DB is migrated at
 * startup). Confirm the key beans exist and the routing datasource falls back
 * to the registry realm when no tenant is selected.
 */
@SpringBootTest
class ApplicationSmokeTest {

    @Autowired
    private DataSource dataSource; // Primary (routing) datasource

    @Autowired
    private JdbcTemplate jdbcTemplate; // School-realm (routing) template

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private SystemDataSourceManager systemDataSourceManager;

    @Test
    void contextLoads() {
        assertThat(dataSource).isNotNull();
        assertThat(jdbcTemplate).isNotNull();
        assertThat(userDetailsService).isNotNull();
        assertThat(systemDataSourceManager).isNotNull();
    }

    @Test
    void routingDatasourceFallsBackToRegistryWithoutTenant() {
        TenantContext.clear();
        // With no routing key, the query must hit the registry DB (has the
        // schools table) rather than a school DB.
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schools", Integer.class);
        assertThat(n).isNotNull();
        TenantContext.clear();
    }
}