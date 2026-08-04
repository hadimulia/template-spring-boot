package com.template.entity.registry;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.extern.slf4j.Slf4j;
import tk.mybatis.mapper.genid.GenId;

/**
 * Generates primary keys for registry tables from their Postgres sequences
 * ({@code <table>_id_seq}) on the {@code sims_registry} connection.
 * <p>
 * Mirror of {@link com.template.entity.PostgreSqlSequenceGenId} but bound to the
 * registry DataSource instead of the routing (school) DataSource: key generation
 * for registry rows must never route to the current school database.
 * <p>
 * Injected by Spring via {@code setJdbcTemplate}.
 */
@Slf4j
public class RegistrySequenceGenId implements GenId<Long> {

    private static JdbcTemplate jdbcTemplate;

    public static void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        RegistrySequenceGenId.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long genId(String table, String column) {
        // Validate table name to prevent SQL injection — only allow alphanumeric and underscores
        if (table == null || !table.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        if (jdbcTemplate == null) {
            throw new IllegalStateException("RegistrySequenceGenId.jdbcTemplate is not set");
        }

        String sql = "SELECT nextval('" + table + "_id_seq')";

        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
