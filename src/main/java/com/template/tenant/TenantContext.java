package com.template.tenant;

/**
 * Holds the tenant id for the current request/thread.
 * Populated by TenantFilter after authentication, consumed by mappers
 * and the AuditInterceptor to scope data access.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        CURRENT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
