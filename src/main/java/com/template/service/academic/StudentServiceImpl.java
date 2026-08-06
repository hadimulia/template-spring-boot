package com.template.service.academic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.dto.PageResult;
import com.template.dto.academic.StudentRequest;
import com.template.dto.academic.StudentResponse;
import com.template.entity.academic.Student;
import com.template.entity.role.Role;
import com.template.entity.user.User;
import com.template.entity.user.UserRole;
import com.template.mapper.academic.StudentMapper;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;
import com.template.mapper.user.UserRoleMapper;
import com.template.registry.mapper.SchoolUserMapper;
import com.template.service.generic.GenericServiceImpl;
import com.template.service.system.LoginAccountHelper;
import com.template.util.SecurityUtils;

import tk.mybatis.mapper.entity.Condition;

@Service
@Transactional
public class StudentServiceImpl extends GenericServiceImpl<Student, Long> implements StudentService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final SchoolUserMapper schoolUserMapper;
    private final LoginAccountHelper loginAccountHelper;

    public StudentServiceImpl(StudentMapper studentMapper, UserMapper userMapper,
                              UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                              SchoolUserMapper schoolUserMapper,
                              LoginAccountHelper loginAccountHelper) {
        super(studentMapper);
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.schoolUserMapper = schoolUserMapper;
        this.loginAccountHelper = loginAccountHelper;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<StudentResponse> findAll(String keyword, int page, int size) {
        Condition cond = new Condition(Student.class);
        cond.createCriteria().andEqualTo("deleted", false);
        if (keyword != null && !keyword.isBlank()) {
            cond.and().andLike("fullname", "%" + keyword + "%");
        }
        int total = studentMapper.selectCountByExample(cond);
        List<Student> students = studentMapper.selectByExample(cond);
        int offsetStart = (page - 1) * size;
        List<StudentResponse> data = students.stream()
                .skip(offsetStart).limit(size)
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResult.of(data, total, page, size);
    }

    @Override
    public void create(StudentRequest request) {
        Long userId = loginAccountHelper.createUserAndIndex(
                request.getNis(), request.getPassword(), request.getFullname(),
                request.getEmail(), userMapper);

        Role role = selectRoleByName("STUDENT");
        if (role != null) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            ur.setCreatedBy(SecurityUtils.getCurrentUsername());
            ur.setCreatedDate(LocalDateTime.now());
            userRoleMapper.insert(ur);
        }

        Student student = new Student();
        student.setUserId(userId);
        student.setNis(request.getNis());
        student.setFullname(request.getFullname());
        student.setGender(request.getGender());
        student.setBirthDate(request.getBirthDate());
        student.setAddress(request.getAddress());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        student.setEnrollmentStatus(request.getEnrollmentStatus() != null ? request.getEnrollmentStatus() : "ACTIVE");
        student.setClassId(request.getClassId());
        studentMapper.insert(student);
    }

    @Override
    public void update(Long id, StudentRequest request) {
        Student student = studentMapper.selectByPrimaryKey(id);
        if (student == null || Boolean.TRUE.equals(student.getDeleted())) {
            return;
        }
        student.setFullname(request.getFullname());
        student.setGender(request.getGender());
        student.setBirthDate(request.getBirthDate());
        student.setAddress(request.getAddress());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        student.setEnrollmentStatus(request.getEnrollmentStatus() != null ? request.getEnrollmentStatus() : student.getEnrollmentStatus());
        student.setClassId(request.getClassId());
        student.setUpdatedBy(SecurityUtils.getCurrentUsername());
        student.setUpdatedDate(LocalDateTime.now());
        studentMapper.updateByPrimaryKey(student);

        if (student.getUserId() != null) {
            User user = userMapper.selectByPrimaryKey(student.getUserId());
            if (user != null) {
                user.setFullname(request.getFullname());
                user.setEmail(request.getEmail());
                user.setUpdatedBy(SecurityUtils.getCurrentUsername());
                user.setUpdatedDate(LocalDateTime.now());
                userMapper.updateByPrimaryKey(user);
            }
        }
    }

    @Override
    public void delete(Long id) {
        Student student = studentMapper.selectByPrimaryKey(id);
        if (student == null) {
            return;
        }
        student.setDeleted(true);
        student.setUpdatedBy(SecurityUtils.getCurrentUsername());
        student.setUpdatedDate(LocalDateTime.now());
        studentMapper.updateByPrimaryKey(student);

        if (student.getUserId() != null) {
            User user = userMapper.selectByPrimaryKey(student.getUserId());
            if (user != null) {
                user.setDeleted(true);
                user.setUpdatedBy(SecurityUtils.getCurrentUsername());
                user.setUpdatedDate(LocalDateTime.now());
                userMapper.updateByPrimaryKey(user);
            }
        }

        Long schoolId = SecurityUtils.getCurrentSchoolId();
        if (schoolId != null && student.getUserId() != null) {
            com.template.entity.registry.SchoolUser index =
                    schoolUserMapper.findByUserIdAndSchool(student.getUserId(), schoolId);
            if (index != null) {
                index.setDeleted(true);
                index.setUpdatedBy(SecurityUtils.getCurrentUsername());
                index.setUpdatedDate(LocalDateTime.now());
                schoolUserMapper.updateByPrimaryKey(index);
            }
        }
    }

    @Override
    public StudentResponse getById(Long id) {
        return toResponse(studentMapper.selectByPrimaryKey(id));
    }

    private Role selectRoleByName(String name) {
        Condition cond = new Condition(Role.class);
        cond.createCriteria().andEqualTo("name", name).andEqualTo("deleted", false);
        return roleMapper.selectOneByExample(cond);
    }

    private StudentResponse toResponse(Student s) {
        if (s == null) {
            return null;
        }
        User u = s.getUserId() != null ? userMapper.selectByPrimaryKey(s.getUserId()) : null;
        return StudentResponse.builder()
                .id(s.getId()).userId(s.getUserId()).nis(s.getNis()).fullname(s.getFullname())
                .gender(s.getGender()).birthDate(s.getBirthDate()).address(s.getAddress())
                .phone(s.getPhone()).email(s.getEmail()).enrollmentStatus(s.getEnrollmentStatus())
                .build();
    }
}