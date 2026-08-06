package com.template.service.academic;

import com.template.dto.PageResult;
import com.template.dto.academic.StudentRequest;
import com.template.dto.academic.StudentResponse;

public interface StudentService {
    PageResult<StudentResponse> findAll(String keyword, int page, int size);
    void create(StudentRequest request);
    void update(Long id, StudentRequest request);
    void delete(Long id);
    StudentResponse getById(Long id);
}