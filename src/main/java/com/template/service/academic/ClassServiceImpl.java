package com.template.service.academic;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.dto.PageResult;
import com.template.dto.academic.ClassRequest;
import com.template.dto.academic.ClassResponse;
import com.template.entity.academic.ClassEntity;
import com.template.entity.academic.ClassStudent;
import com.template.entity.academic.Teacher;
import com.template.mapper.academic.ClassMapper;
import com.template.mapper.academic.ClassStudentMapper;
import com.template.mapper.academic.TeacherMapper;
import com.template.service.generic.GenericServiceImpl;
import com.template.util.SecurityUtils;

import tk.mybatis.mapper.entity.Condition;

@Service
@Transactional
public class ClassServiceImpl extends GenericServiceImpl<ClassEntity, Long> implements ClassService {

    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;
    private final TeacherMapper teacherMapper;

    public ClassServiceImpl(ClassMapper classMapper, ClassStudentMapper classStudentMapper,
                            TeacherMapper teacherMapper) {
        super(classMapper);
        this.classMapper = classMapper;
        this.classStudentMapper = classStudentMapper;
        this.teacherMapper = teacherMapper;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<ClassResponse> findAll(String keyword, int page, int size) {
        Condition cond = new Condition(ClassEntity.class);
        cond.createCriteria().andEqualTo("deleted", false);
        if (keyword != null && !keyword.isBlank()) {
            cond.and().andLike("name", "%" + keyword + "%");
        }
        int total = classMapper.selectCountByExample(cond);
        List<ClassEntity> classes = classMapper.selectByExample(cond);
        int offsetStart = (page - 1) * size;
        List<ClassResponse> data = classes.stream()
                .skip(offsetStart).limit(size)
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResult.of(data, total, page, size);
    }

    @Override
    public void create(ClassRequest request) {
        ClassEntity c = new ClassEntity();
        c.setName(request.getName());
        c.setGrade(request.getGrade());
        c.setAcademicYear(request.getAcademicYear());
        c.setHomeroomTeacherId(request.getHomeroomTeacherId());
        classMapper.insert(c);
        replaceStudents(c.getId(), request.getStudentIds());
    }

    @Override
    public void update(Long id, ClassRequest request) {
        ClassEntity c = classMapper.selectByPrimaryKey(id);
        if (c == null || Boolean.TRUE.equals(c.getDeleted())) {
            return;
        }
        c.setName(request.getName());
        c.setGrade(request.getGrade());
        c.setAcademicYear(request.getAcademicYear());
        c.setHomeroomTeacherId(request.getHomeroomTeacherId());
        c.setUpdatedBy(SecurityUtils.getCurrentUsername());
        c.setUpdatedDate(LocalDateTime.now());
        classMapper.updateByPrimaryKey(c);
        replaceStudents(id, request.getStudentIds());
    }

    @Override
    public void delete(Long id) {
        ClassEntity c = classMapper.selectByPrimaryKey(id);
        if (c == null) {
            return;
        }
        c.setDeleted(true);
        c.setUpdatedBy(SecurityUtils.getCurrentUsername());
        c.setUpdatedDate(LocalDateTime.now());
        classMapper.updateByPrimaryKey(c);
    }

    @Override
    public ClassResponse getById(Long id) {
        return toResponse(classMapper.selectByPrimaryKey(id));
    }

    /** Replaces the class↔student links for a class. */
    private void replaceStudents(Long classId, List<Long> studentIds) {
        Condition del = new Condition(ClassStudent.class);
        del.createCriteria().andEqualTo("classId", classId);
        classStudentMapper.deleteByExample(del);

        if (studentIds == null) {
            return;
        }
        for (Long sid : studentIds) {
            ClassStudent cs = new ClassStudent();
            cs.setClassId(classId);
            cs.setStudentId(sid);
            cs.setCreatedBy(SecurityUtils.getCurrentUsername());
            cs.setCreatedDate(LocalDateTime.now());
            classStudentMapper.insert(cs);
        }
    }

    private ClassResponse toResponse(ClassEntity c) {
        if (c == null) {
            return null;
        }
        String teacherName = null;
        if (c.getHomeroomTeacherId() != null) {
            Teacher t = teacherMapper.selectByPrimaryKey(c.getHomeroomTeacherId());
            if (t != null && !Boolean.TRUE.equals(t.getDeleted())) {
                teacherName = t.getFullname();
            }
        }

        Condition csCond = new Condition(ClassStudent.class);
        csCond.createCriteria().andEqualTo("classId", c.getId()).andEqualTo("deleted", false);
        List<Long> studentIds = new ArrayList<>();
        for (ClassStudent cs : classStudentMapper.selectByExample(csCond)) {
            studentIds.add(cs.getStudentId());
        }

        return ClassResponse.builder()
                .id(c.getId()).name(c.getName()).grade(c.getGrade())
                .academicYear(c.getAcademicYear()).homeroomTeacherId(c.getHomeroomTeacherId())
                .homeroomTeacherName(teacherName).studentIds(studentIds)
                .studentCount(studentIds.size())
                .build();
    }
}