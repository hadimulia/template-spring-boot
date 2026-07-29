package com.template.mapper.permission;

import com.template.entity.permission.RolePermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface RolePermissionMapper extends Mapper<RolePermission> {

    @Select("SELECT permission_id FROM role_permissions WHERE role_id = #{roleId}")
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
