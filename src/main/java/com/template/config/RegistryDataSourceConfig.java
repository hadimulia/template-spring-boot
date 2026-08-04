package com.template.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * The shared registry database ({@code sims_registry}) holding the school list
 * and the global login index. Its Flyway migration set lives in
 * {@code classpath:db/registry}.
 */
@Configuration
public class RegistryDataSourceConfig {

    @Bean(destroyMethod = "close")
    public HikariDataSource registryDataSource(RegistryDataSourceProperties props) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setDriverClassName(props.getDriverClassName());
        config.setPoolName("registry-pool");
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    /** Registry JdbcTemplate, bound to sims_registry. Used by RegistrySequenceGenId. */
    @Bean
    public JdbcTemplate registryJdbcTemplate(@Qualifier("registryDataSource") DataSource registryDataSource) {
        return new JdbcTemplate(registryDataSource);
    }

    /**
     * Migrate the registry database on startup. Must run before any registry
     * mapper is used, so it is created eagerly.
     */
    @Bean(initMethod = "migrate")
    public Flyway registryFlyway(@Qualifier("registryDataSource") DataSource registryDataSource) {
        return Flyway.configure()
                .dataSource(registryDataSource)
                .locations("classpath:db/registry")
                .baselineOnMigrate(true)
                .load();
    }
}
