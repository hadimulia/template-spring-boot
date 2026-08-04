package com.template.config;

import com.template.entity.BaseEntity;
import com.template.util.SecurityUtils;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

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
            } else if (sqlType == SqlCommandType.UPDATE) {
                entity.setUpdatedBy(username);
                entity.setUpdatedDate(LocalDateTime.now());
            }
        }

        return invocation.proceed();
    }
}
