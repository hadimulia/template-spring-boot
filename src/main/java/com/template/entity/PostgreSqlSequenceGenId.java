package com.template.entity;

import org.springframework.jdbc.core.JdbcTemplate;

import tk.mybatis.mapper.genid.GenId;

public class PostgreSqlSequenceGenId implements GenId<Long>{

    /**
     * Injected by Spring.
     */
    private static JdbcTemplate jdbcTemplate;

    public static void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        PostgreSqlSequenceGenId.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long genId(String table, String column) {
        // Validate table name to prevent SQL injection — only allow alphanumeric and underscores
        if (table == null || !table.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        String sql = "SELECT nextval('" + table + "_id_seq')";

        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
