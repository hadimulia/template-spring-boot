package com.template.service.audit;

import com.template.dto.PageResult;
import com.template.dto.audit.AuditLogResponse;

public interface AuditService {
    void record(String action, String entityType, Long entityId, String description);
    void record(String action, String entityType, Long entityId, String description, String oldValue, String newValue);
    PageResult<AuditLogResponse> findAll(String keyword, int page, int size);
}
