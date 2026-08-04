package com.template.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Separate SqlSessionFactory for the registry realm ({@code sims_registry}).
 * Registry mappers (currently {@code com.template.registry.mapper}) are bound to
 * this factory so they always hit the registry database regardless of the
 * request's routing key. Mapper XMLs live under {@code classpath:registry-mapper}.
 * <p>
 * Every parameter reference is {@link Qualifier}ed explicitly: the school realm's
 * factory/template/datasource are all {@code @Primary}, and Spring's autowiring
 * prefers {@code @Primary} over parameter names, so without qualifiers the
 * registry beans would silently bind to the school realm.
 */
@Configuration
public class RegistrySqlSessionFactoryConfig {

    @Bean
    public SqlSessionFactory registrySqlSessionFactory(@Qualifier("registryDataSource") DataSource registryDataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(registryDataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:registry-mapper/**/*.xml"));
        return factory.getObject();
    }

    @Bean
    public SqlSessionTemplate registrySqlSessionTemplate(@Qualifier("registrySqlSessionFactory") SqlSessionFactory registrySqlSessionFactory) {
        return new SqlSessionTemplate(registrySqlSessionFactory);
    }

    @Bean
    public PlatformTransactionManager registryTransactionManager(@Qualifier("registryDataSource") DataSource registryDataSource) {
        return new DataSourceTransactionManager(registryDataSource);
    }
}
