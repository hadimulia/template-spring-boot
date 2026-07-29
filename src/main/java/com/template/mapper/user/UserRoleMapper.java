package com.template.mapper.user;

import com.template.entity.user.UserRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface UserRoleMapper extends Mapper<UserRole> {

    @Select("SELECT role_id FROM user_roles WHERE user_id = #{userId} AND deleted = false")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT role_id FROM user_roles WHERE user_id = #{userId}")
    List<Long> findRoleIdsByUserIdAll(@Param("userId") Long userId);
}
