package com.template.mapper.permission;

import com.template.dto.permission.PermissionResponse;
import com.template.entity.permission.Permission;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface PermissionMapper extends Mapper<Permission> {

    List<Permission> findByRoleId(@Param("roleId") Long roleId);

    List<Permission> findByRoleNames(@Param("roleNames") List<String> roleNames);

    List<PermissionResponse> findAll(@Param("keyword") String keyword,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    int countAll(@Param("keyword") String keyword);
}
