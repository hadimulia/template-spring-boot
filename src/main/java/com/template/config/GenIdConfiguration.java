package com.template.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.template.entity.PostgreSqlSequenceGenId;

import jakarta.annotation.PostConstruct;

@Configuration
public class GenIdConfiguration {

    private final JdbcTemplate jdbcTemplate;

    public GenIdConfiguration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        PostgreSqlSequenceGenId.setJdbcTemplate(jdbcTemplate);
    }
}
