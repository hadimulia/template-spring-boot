package com.template.mapper.role;

import com.template.dto.role.RoleResponse;
import com.template.entity.role.Role;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface RoleMapper extends Mapper<Role> {

    List<Role> findByUserId(@Param("userId") Long userId);

    List<RoleResponse> findAll(@Param("keyword") String keyword,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    int countAll(@Param("keyword") String keyword);
}
