package com.template.service.academic;

import com.template.dto.PageResult;
import com.template.dto.academic.ClassRequest;
import com.template.dto.academic.ClassResponse;

public interface ClassService {
    PageResult<ClassResponse> findAll(String keyword, int page, int size);
    void create(ClassRequest request);
    void update(Long id, ClassRequest request);
    void delete(Long id);
    ClassResponse getById(Long id);
}