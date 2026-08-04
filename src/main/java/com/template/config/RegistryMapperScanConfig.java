package com.template.config;

import org.springframework.context.annotation.Configuration;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * Scans registry-realm mappers and binds them to the registry SqlSessionTemplate,
 * so they always query {@code sims_registry} independent of the routing key.
 * Uses the same {@code mapper.*} configuration (generic Mapper, POSTGRES identity)
 * as the school realm via the starter's environment binding.
 */
@Configuration
@MapperScan(basePackages = "com.template.registry.mapper",
        sqlSessionTemplateRef = "registrySqlSessionTemplate")
public class RegistryMapperScanConfig {
}
