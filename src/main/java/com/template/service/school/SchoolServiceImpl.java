package com.template.service.school;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.config.SchoolDataSourceManager;
import com.template.dto.PageResult;
import com.template.dto.school.SchoolRequest;
import com.template.dto.school.SchoolResponse;
import com.template.entity.school.School;
import com.template.registry.mapper.SchoolMapper;
import com.template.service.audit.Auditable;
import com.template.service.generic.GenericServiceImpl;
import com.template.util.SecurityUtils;

@Service
@Transactional
public class SchoolServiceImpl extends GenericServiceImpl<School, Long> implements SchoolService {

    private final SchoolMapper schoolMapper;
    private final SchoolDataSourceManager schoolDataSourceManager;

    public SchoolServiceImpl(SchoolMapper schoolMapper, SchoolDataSourceManager schoolDataSourceManager) {
        super(schoolMapper);
        this.schoolMapper = schoolMapper;
        this.schoolDataSourceManager = schoolDataSourceManager;
    }

    @Transactional(readOnly = true)
    public School getByCode(String code) {
        return schoolMapper.findByCode(code);
    }

    @Transactional(readOnly = true)
    public PageResult<SchoolResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<SchoolResponse> data = schoolMapper.findAll(keyword, offset, size);
        int total = schoolMapper.countAll(keyword);
        return PageResult.of(data, total, page, size);
    }

    @Auditable(action = "CREATE", entityType = "SCHOOL", description = "#request.code")
    public void create(SchoolRequest request) {
        if (schoolMapper.findByCode(request.getCode()) != null) {
            throw new IllegalArgumentException("School code already exists: " + request.getCode());
        }
        // Auto-provision the school database BEFORE persisting the registry row so
        // a failure leaves no orphan registry entry.
        String dbName = schoolDataSourceManager.databaseName(request.getCode());
        schoolDataSourceManager.getOrCreate(request.getCode());

        School school = new School();
        school.setCode(request.getCode());
        school.setName(request.getName());
        school.setDescription(request.getDescription());
        school.setStatus(request.getStatus());
        school.setDbName(dbName);
        school.setCreatedBy(SecurityUtils.getCurrentUsername());
        school.setCreatedDate(LocalDateTime.now());
        school.setDeleted(false);
        school.setVersion(0);
        save(school);
    }

    @Auditable(action = "UPDATE", entityType = "SCHOOL", description = "#request.code")
    public void update(Long id, SchoolRequest request) {
        School school = get(id);
        if (school != null) {
            school.setName(request.getName());
            school.setDescription(request.getDescription());
            school.setStatus(request.getStatus());
            school.setUpdatedBy(SecurityUtils.getCurrentUsername());
            school.setUpdatedDate(LocalDateTime.now());
            save(school);
        }
    }

    @Auditable(action = "DELETE", entityType = "SCHOOL", description = "")
    public void delete(Long id) {
        School school = get(id);
        if (school != null) {
            school.setStatus("INACTIVE");
            school.setDeleted(true);
            school.setUpdatedBy(SecurityUtils.getCurrentUsername());
            school.setUpdatedDate(LocalDateTime.now());
            save(school);
        }
    }

    @Transactional(readOnly = true)
    public SchoolResponse getById(Long id) {
        School school = get(id);
        if (school == null || Boolean.TRUE.equals(school.getDeleted())) return null;

        return SchoolResponse.builder()
                .id(school.getId())
                .code(school.getCode())
                .name(school.getName())
                .dbName(school.getDbName())
                .description(school.getDescription())
                .status(school.getStatus())
                .createdBy(school.getCreatedBy())
                .createdDate(school.getCreatedDate())
                .updatedBy(school.getUpdatedBy())
                .updatedDate(school.getUpdatedDate())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> findAll() {
        return getAll().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .map(s -> SchoolResponse.builder()
                        .id(s.getId())
                        .code(s.getCode())
                        .name(s.getName())
                        .dbName(s.getDbName())
                        .status(s.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
