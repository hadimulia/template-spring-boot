package com.template.mapper.audit;

import com.template.dto.audit.AuditLogResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuditLogMapper {

    List<AuditLogResponse> findPage(@Param("keyword") String keyword,
                                    @Param("tenantId") Long tenantId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    int countPage(@Param("keyword") String keyword, @Param("tenantId") Long tenantId);

    void insert(com.template.entity.audit.AuditLog auditLog);
}
