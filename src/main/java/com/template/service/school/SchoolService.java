package com.template.service.school;

import com.template.dto.PageResult;
import com.template.dto.school.SchoolRequest;
import com.template.dto.school.SchoolResponse;
import com.template.entity.school.School;
import com.template.service.generic.GenericService;

import java.util.List;

public interface SchoolService extends GenericService<School, Long> {

    School getByCode(String code);

    PageResult<SchoolResponse> findAll(String keyword, int page, int size);

    void create(SchoolRequest request);

    void update(Long id, SchoolRequest request);

    void delete(Long id);

    SchoolResponse getById(Long id);

    List<SchoolResponse> findAll();
}
