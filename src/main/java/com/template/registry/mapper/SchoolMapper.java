package com.template.registry.mapper;

import com.template.dto.school.SchoolResponse;
import com.template.entity.school.School;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * Registry-realm mapper for the {@code schools} table in {@code sims_registry}.
 * Read by the school-onboarding flow; NOT part of the school realm scan.
 */
public interface SchoolMapper extends Mapper<School> {

    School findByCode(@Param("code") String code);

    School findByDbName(@Param("dbName") String dbName);

    List<SchoolResponse> findAll(@Param("keyword") String keyword,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    int countAll(@Param("keyword") String keyword);
}
