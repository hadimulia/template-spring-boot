package com.template.mapper.user;

import com.template.dto.user.UserResponse;
import com.template.entity.user.User;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface UserMapper extends Mapper<User> {

    User findByUsername(@Param("username") String username, @Param("tenantId") Long tenantId);

    User findByEmail(@Param("email") String email, @Param("tenantId") Long tenantId);

    List<UserResponse> findAll(@Param("keyword") String keyword,
                               @Param("tenantId") Long tenantId,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    int countAll(@Param("keyword") String keyword, @Param("tenantId") Long tenantId);

    int countByTenant(@Param("tenantId") Long tenantId);
}
