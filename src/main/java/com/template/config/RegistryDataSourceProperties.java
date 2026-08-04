package com.template.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Connection info for the shared registry database that holds the school list
 * and the global login index. Bound from {@code spring.datasource.registry.*}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.datasource.registry")
public class RegistryDataSourceProperties {

    private String dbName = "sims_registry";
    private String url = "jdbc:postgresql://localhost:5432/sims_registry";
    private String username = "postgres";
    private String password = "postgres";
    private String driverClassName = "org.postgresql.Driver";
}
