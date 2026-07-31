package com.template.config;

import com.template.entity.BaseEntity;
import com.template.entity.audit.AuditLog;
import com.template.entity.approval.ApprovalRequest;
import com.template.tenant.TenantContext;
import com.template.util.SecurityUtils;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AuditInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        if (parameter instanceof BaseEntity entity) {
            SqlCommandType sqlType = ms.getSqlCommandType();
            String username = SecurityUtils.getCurrentUsername();

            if (sqlType == SqlCommandType.INSERT) {
                entity.setCreatedBy(username);
                entity.setCreatedDate(LocalDateTime.now());
                entity.setUpdatedBy(username);
                entity.setUpdatedDate(LocalDateTime.now());
                if (entity.getDeleted() == null) {
                    entity.setDeleted(false);
                }
                if (entity.getVersion() == null) {
                    entity.setVersion(0);
                }
                setTenantIdIfPresent(entity);
            } else if (sqlType == SqlCommandType.UPDATE) {
                entity.setUpdatedBy(username);
                entity.setUpdatedDate(LocalDateTime.now());
            }
        }

        if (parameter instanceof AuditLog auditLog) {
            if (ms.getSqlCommandType() == SqlCommandType.INSERT && auditLog.getTenantId() == null) {
                auditLog.setTenantId(TenantContext.getTenantId());
            }
        }

        if (parameter instanceof ApprovalRequest approvalRequest) {
            if (ms.getSqlCommandType() == SqlCommandType.INSERT && approvalRequest.getTenantId() == null) {
                approvalRequest.setTenantId(TenantContext.getTenantId());
            }
        }

        return invocation.proceed();
    }

    private void setTenantIdIfPresent(Object entity) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        for (Class<?> clazz = entity.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field field = clazz.getDeclaredField("tenantId");
                field.setAccessible(true);
                if (field.get(entity) == null) {
                    field.set(entity, tenantId);
                }
                return;
            } catch (NoSuchFieldException ignored) {
                // continue up the hierarchy
            } catch (IllegalAccessException ignored) {
                return;
            }
        }
    }
}
