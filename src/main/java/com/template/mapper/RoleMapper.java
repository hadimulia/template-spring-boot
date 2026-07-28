package com.template.mapper;

import com.template.dto.RoleResponse;
import com.template.entity.Role;
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
