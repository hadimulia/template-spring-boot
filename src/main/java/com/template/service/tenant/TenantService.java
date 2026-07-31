package com.template.service.tenant;

import com.template.dto.PageResult;
import com.template.dto.tenant.TenantRequest;
import com.template.dto.tenant.TenantResponse;
import com.template.entity.tenant.Tenant;
import com.template.service.generic.GenericService;

import java.util.List;

public interface TenantService extends GenericService<Tenant, Long> {

    Tenant getByCode(String code);

    PageResult<TenantResponse> findAll(String keyword, int page, int size);

    void create(TenantRequest request);

    void update(Long id, TenantRequest request);

    void delete(Long id);

    TenantResponse getById(Long id);

    List<TenantResponse> findAll();
}
