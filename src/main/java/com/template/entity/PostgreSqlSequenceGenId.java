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

        String sequence = table + "_id_seq";

        String sql = "SELECT nextval('" + sequence + "')";

        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
