package com.template.service.academic;

import com.template.dto.PageResult;
import com.template.dto.academic.TeacherRequest;
import com.template.dto.academic.TeacherResponse;

public interface TeacherService {
    PageResult<TeacherResponse> findAll(String keyword, int page, int size);
    void create(TeacherRequest request);
    void update(Long id, TeacherRequest request);
    void delete(Long id);
    TeacherResponse getById(Long id);
}