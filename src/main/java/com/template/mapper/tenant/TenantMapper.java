package com.template.mapper.tenant;

import com.template.dto.tenant.TenantResponse;
import com.template.entity.tenant.Tenant;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface TenantMapper extends Mapper<Tenant> {

    Tenant findByCode(@Param("code") String code);

    List<TenantResponse> findAll(@Param("keyword") String keyword,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    int countAll(@Param("keyword") String keyword);
}
