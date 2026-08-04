package com.template.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Wires the routing DataSource as the application's primary DataSource. MyBatis,
 * Hikari auto-config, and the default tk.mybatis SqlSessionFactory all bind to it,
 * so school-realm mappers execute against the routed school database.
 */
@Configuration
public class RoutingDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSource registryDataSource, SchoolDataSourceManager manager) {
        return new TenantDataSource(registryDataSource, manager);
    }

    /**
     * School-realm JdbcTemplate, bound to the routing DataSource. Declared
     * explicitly because Boot's JdbcTemplateAutoConfiguration is
     * {@code @ConditionalOnSingleCandidate(DataSource)} and backs off once the
     * registry DataSource introduces a second candidate.
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
