package com.template.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Explicit {@link SqlSessionFactory} for the school realm. The tk.mybatis
 * auto-configuration backs off whenever a single {@link SqlSessionFactory} bean
 * already exists (it is {@code @ConditionalOnMissingBean}), so once the registry
 * factory was introduced the school factory had to be declared here too.
 * <p>
 * This factory binds to the routing {@link DataSource} ({@link TenantDataSource}),
 * loads the school mapper XMLs from {@code classpath:mapper/**\/\*.xml} and
 * registers the tk.mybatis generic {@link Mapper} helper so the auto CRUD methods
 * keep working on school-realm entities.
 */
@Configuration
public class SchoolSqlSessionFactoryConfig {

    private final AuditInterceptor auditInterceptor;

    public SchoolSqlSessionFactoryConfig(AuditInterceptor auditInterceptor) {
        this.auditInterceptor = auditInterceptor;
    }

    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/**/*.xml"));
        factory.setPlugins(new org.apache.ibatis.plugin.Interceptor[]{auditInterceptor});
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);
        return factory.getObject();
    }

    @Bean
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
