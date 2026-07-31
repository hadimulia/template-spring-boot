package com.template.service.audit;

import com.template.dto.PageResult;
import com.template.dto.audit.AuditLogResponse;
import com.template.entity.audit.AuditLog;
import com.template.mapper.audit.AuditLogMapper;
import com.template.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void record(String action, String entityType, Long entityId, String description) {
        record(action, entityType, entityId, description, null, null);
    }

    @Override
    public void record(String action, String entityType, Long entityId, String description, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setPerformedBy(com.template.util.SecurityUtils.getCurrentUsername());
        log.setPerformedAt(LocalDateTime.now());
        log.setIpAddress(com.template.util.SecurityUtils.getCurrentIpAddress());
        auditLogMapper.insert(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditLogResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        Long tenantId = TenantContext.getTenantId();
        List<AuditLogResponse> data = auditLogMapper.findPage(keyword, tenantId, offset, size);
        int total = auditLogMapper.countPage(keyword, tenantId);
        return PageResult.of(data, total, page, size);
    }
}
