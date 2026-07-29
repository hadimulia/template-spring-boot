package com.template.mapper.role;

import com.template.entity.role.RoleMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface RoleMenuMapper extends Mapper<RoleMenu> {

    @Select("SELECT menu_id FROM role_menus WHERE role_id = #{roleId}")
    List<Long> findMenuIdsByRoleId(@Param("roleId") Long roleId);
}
