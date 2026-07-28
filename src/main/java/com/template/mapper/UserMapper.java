package com.template.mapper;

import com.template.dto.UserResponse;
import com.template.entity.User;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface UserMapper extends Mapper<User> {

    User findByUsername(@Param("username") String username);

    List<UserResponse> findAll(@Param("keyword") String keyword,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    int countAll(@Param("keyword") String keyword);
}
