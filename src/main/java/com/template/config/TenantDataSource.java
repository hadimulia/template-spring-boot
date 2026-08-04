package com.template.config;

import com.template.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes every JDBC request to the authenticated school's database. The lookup
 * key is the school's physical db name from {@link TenantContext}. When no key
 * is set (pre-login, registry-only pages, login processing) the connection falls
 * back to the registry database.
 * <p>
 * School pools are created lazily by {@link SchoolDataSourceManager} on first use
 * and cached here.
 */
public class TenantDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantDataSource.class);

    private final DataSource registryDataSource;
    private final SchoolDataSourceManager manager;
    private final Map<String, DataSource> schoolDataSources = new ConcurrentHashMap<>();

    public TenantDataSource(DataSource registryDataSource, SchoolDataSourceManager manager) {
        this.registryDataSource = registryDataSource;
        this.manager = manager;
        setDefaultTargetDataSource(registryDataSource);
        setTargetDataSources(new ConcurrentHashMap<>());
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String routingKey = TenantContext.getRoutingKey();
        if (routingKey == null || routingKey.isBlank()) {
            return null;
        }
        return routingKey;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object key = determineCurrentLookupKey();
        if (key == null) {
            return registryDataSource;
        }
        String dbName = (String) key;
        return schoolDataSources.computeIfAbsent(dbName, name -> {
            log.info("Initializing school database {}", name);
            return manager.getOrCreateByDbName(name);
        });
    }

    /** Allows the registry realm to be exposed separately if needed. */
    public DataSource registryDataSource() {
        return registryDataSource;
    }
}
