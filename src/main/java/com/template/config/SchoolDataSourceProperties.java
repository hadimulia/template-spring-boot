package com.template.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Connection template for school databases. A per-school Hikari pool is built by
 * substituting the school's db name into the URL prefix. Bound from
 * {@code spring.datasource.school.*}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.datasource.school")
public class SchoolDataSourceProperties {

    /**
     * Base JDBC URL prefix, e.g. {@code jdbc:postgresql://localhost:5432/}.
     * The school db name ({@code sims_<code>}) is appended.
     */
    private String urlPrefix = "jdbc:postgresql://localhost:5432/";

    /**
     * Maintenance database used to run {@code CREATE DATABASE}. The connecting
     * user must have the {@code CREATEDB} privilege.
     */
    private String adminUrl = "jdbc:postgresql://localhost:5432/postgres";

    private String username = "postgres";
    private String password = "postgres";
    private String driverClassName = "org.postgresql.Driver";

    /** Max active connections per school pool. */
    private int maxPoolSize = 10;
}
