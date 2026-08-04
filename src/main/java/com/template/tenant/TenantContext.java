package com.template.tenant;

/**
 * Holds the school (tenant) routing context for the current request/thread.
 * <p>
 * {@code routingKey} is the school database name (e.g. {@code sims_default}) used
 * by the routing DataSource to pick the physical database. {@code tenantId} is the
 * numeric registry id of the school, used for lookups. Both are populated by the
 * filter/security layer after authentication and cleared when the request finishes
 * so no state leaks across threads.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROUTING_KEY = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void setRoutingKey(String routingKey) {
        ROUTING_KEY.set(routingKey);
    }

    public static String getRoutingKey() {
        return ROUTING_KEY.get();
    }

    public static void clear() {
        TENANT_ID.remove();
        ROUTING_KEY.remove();
    }
}
