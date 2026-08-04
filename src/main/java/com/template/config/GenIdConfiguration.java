package com.template.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.template.entity.PostgreSqlSequenceGenId;
import com.template.entity.registry.RegistrySequenceGenId;

import jakarta.annotation.PostConstruct;

@Configuration
class GenIdConfiguration {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate registryJdbcTemplate;

    GenIdConfiguration(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate,
                       @Qualifier("registryJdbcTemplate") JdbcTemplate registryJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.registryJdbcTemplate = registryJdbcTemplate;
    }

    @PostConstruct
    void init() {
        PostgreSqlSequenceGenId.setJdbcTemplate(jdbcTemplate);
        RegistrySequenceGenId.setJdbcTemplate(registryJdbcTemplate);
    }
}
