package com.template.registry.mapper;

import com.template.entity.registry.SchoolUser;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

/**
 * Registry-realm mapper for the global login index ({@code school_users}).
 */
public interface SchoolUserMapper extends Mapper<SchoolUser> {

    SchoolUser findByUsername(@Param("username") String username);

    SchoolUser findByUserIdAndSchool(@Param("userId") Long userId, @Param("schoolId") Long schoolId);
}
