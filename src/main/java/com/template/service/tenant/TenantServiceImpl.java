package com.template.service.tenant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.dto.PageResult;
import com.template.dto.tenant.TenantRequest;
import com.template.dto.tenant.TenantResponse;
import com.template.entity.tenant.Tenant;
import com.template.mapper.tenant.TenantMapper;
import com.template.service.audit.Auditable;
import com.template.service.generic.GenericServiceImpl;
import com.template.util.SecurityUtils;

@Service
@Transactional
public class TenantServiceImpl extends GenericServiceImpl<Tenant, Long> implements TenantService {

    private final TenantMapper tenantMapper;

    public TenantServiceImpl(TenantMapper tenantMapper) {
        super(tenantMapper);
        this.tenantMapper = tenantMapper;
    }

    @Transactional(readOnly = true)
    public Tenant getByCode(String code) {
        return tenantMapper.findByCode(code);
    }

    @Transactional(readOnly = true)
    public PageResult<TenantResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<TenantResponse> data = tenantMapper.findAll(keyword, offset, size);
        int total = tenantMapper.countAll(keyword);
        return PageResult.of(data, total, page, size);
    }

    @Auditable(action = "CREATE", entityType = "TENANT", description = "#request.code")
    public void create(TenantRequest request) {
        Tenant tenant = new Tenant();
        tenant.setCode(request.getCode());
        tenant.setName(request.getName());
        tenant.setDescription(request.getDescription());
        tenant.setStatus(request.getStatus());
        tenant.setCreatedBy(SecurityUtils.getCurrentUsername());
        tenant.setCreatedDate(LocalDateTime.now());
        tenant.setDeleted(false);
        tenant.setVersion(0);
        save(tenant);
    }

    @Auditable(action = "UPDATE", entityType = "TENANT", description = "#request.code")
    public void update(Long id, TenantRequest request) {
        Tenant tenant = get(id);
        if (tenant != null) {
            tenant.setCode(request.getCode());
            tenant.setName(request.getName());
            tenant.setDescription(request.getDescription());
            tenant.setStatus(request.getStatus());
            tenant.setUpdatedBy(SecurityUtils.getCurrentUsername());
            tenant.setUpdatedDate(LocalDateTime.now());
            save(tenant);
        }
    }

    @Auditable(action = "DELETE", entityType = "TENANT", description = "")
    public void delete(Long id) {
        Tenant tenant = get(id);
        if (tenant != null) {
            tenant.setDeleted(true);
            tenant.setUpdatedBy(SecurityUtils.getCurrentUsername());
            tenant.setUpdatedDate(LocalDateTime.now());
            save(tenant);
        }
    }

    @Transactional(readOnly = true)
    public TenantResponse getById(Long id) {
        Tenant tenant = get(id);
        if (tenant == null || Boolean.TRUE.equals(tenant.getDeleted())) return null;

        return TenantResponse.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .description(tenant.getDescription())
                .status(tenant.getStatus())
                .createdBy(tenant.getCreatedBy())
                .createdDate(tenant.getCreatedDate())
                .updatedBy(tenant.getUpdatedBy())
                .updatedDate(tenant.getUpdatedDate())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> findAll() {
        return getAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .map(t -> TenantResponse.builder()
                        .id(t.getId())
                        .code(t.getCode())
                        .name(t.getName())
                        .status(t.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
