package com.template.service.academic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.dto.PageResult;
import com.template.dto.academic.TeacherRequest;
import com.template.dto.academic.TeacherResponse;
import com.template.entity.academic.Teacher;
import com.template.entity.role.Role;
import com.template.entity.user.User;
import com.template.entity.user.UserRole;
import com.template.mapper.academic.TeacherMapper;
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
public class TeacherServiceImpl extends GenericServiceImpl<Teacher, Long> implements TeacherService {

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final SchoolUserMapper schoolUserMapper;
    private final LoginAccountHelper loginAccountHelper;

    public TeacherServiceImpl(TeacherMapper teacherMapper, UserMapper userMapper,
                              UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                              SchoolUserMapper schoolUserMapper,
                              LoginAccountHelper loginAccountHelper) {
        super(teacherMapper);
        this.teacherMapper = teacherMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.schoolUserMapper = schoolUserMapper;
        this.loginAccountHelper = loginAccountHelper;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<TeacherResponse> findAll(String keyword, int page, int size) {
        Condition cond = new Condition(Teacher.class);
        cond.createCriteria().andEqualTo("deleted", false);
        if (keyword != null && !keyword.isBlank()) {
            cond.and().andLike("fullname", "%" + keyword + "%");
        }
        int total = teacherMapper.selectCountByExample(cond);
        List<Teacher> teachers = teacherMapper.selectByExample(cond);
        // simple paging on the in-memory list (kept small); refine with LIMIT if large
        int offsetStart = (page - 1) * size;
        List<TeacherResponse> data = teachers.stream()
                .skip(offsetStart).limit(size)
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResult.of(data, total, page, size);
    }

    @Override
    public void create(TeacherRequest request) {
        Long userId = loginAccountHelper.createUserAndIndex(
                request.getNip(), request.getPassword(), request.getFullname(),
                request.getEmail(), userMapper);

        Role role = selectRoleByName("TEACHER");
        if (role != null) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            ur.setCreatedBy(SecurityUtils.getCurrentUsername());
            ur.setCreatedDate(LocalDateTime.now());
            userRoleMapper.insert(ur);
        }

        Teacher teacher = new Teacher();
        teacher.setUserId(userId);
        teacher.setNip(request.getNip());
        teacher.setFullname(request.getFullname());
        teacher.setGender(request.getGender());
        teacher.setBirthDate(request.getBirthDate());
        teacher.setAddress(request.getAddress());
        teacher.setPhone(request.getPhone());
        teacher.setEmail(request.getEmail());
        teacher.setHireDate(request.getHireDate());
        teacherMapper.insert(teacher);
    }

    @Override
    public void update(Long id, TeacherRequest request) {
        Teacher teacher = teacherMapper.selectByPrimaryKey(id);
        if (teacher == null || Boolean.TRUE.equals(teacher.getDeleted())) {
            return;
        }
        teacher.setFullname(request.getFullname());
        teacher.setGender(request.getGender());
        teacher.setBirthDate(request.getBirthDate());
        teacher.setAddress(request.getAddress());
        teacher.setPhone(request.getPhone());
        teacher.setEmail(request.getEmail());
        teacher.setHireDate(request.getHireDate());
        teacher.setUpdatedBy(SecurityUtils.getCurrentUsername());
        teacher.setUpdatedDate(LocalDateTime.now());
        teacherMapper.updateByPrimaryKey(teacher);

        if (teacher.getUserId() != null) {
            User user = userMapper.selectByPrimaryKey(teacher.getUserId());
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
        Teacher teacher = teacherMapper.selectByPrimaryKey(id);
        if (teacher == null) {
            return;
        }
        teacher.setDeleted(true);
        teacher.setUpdatedBy(SecurityUtils.getCurrentUsername());
        teacher.setUpdatedDate(LocalDateTime.now());
        teacherMapper.updateByPrimaryKey(teacher);

        if (teacher.getUserId() != null) {
            User user = userMapper.selectByPrimaryKey(teacher.getUserId());
            if (user != null) {
                user.setDeleted(true);
                user.setUpdatedBy(SecurityUtils.getCurrentUsername());
                user.setUpdatedDate(LocalDateTime.now());
                userMapper.updateByPrimaryKey(user);
            }
        }

        Long schoolId = SecurityUtils.getCurrentSchoolId();
        if (schoolId != null) {
            com.template.entity.registry.SchoolUser index =
                    schoolUserMapper.findByUserIdAndSchool(teacher.getUserId(), schoolId);
            if (index != null) {
                index.setDeleted(true);
                index.setUpdatedBy(SecurityUtils.getCurrentUsername());
                index.setUpdatedDate(LocalDateTime.now());
                schoolUserMapper.updateByPrimaryKey(index);
            }
        }
    }

    @Override
    public TeacherResponse getById(Long id) {
        return toResponse(teacherMapper.selectByPrimaryKey(id));
    }

    private Role selectRoleByName(String name) {
        Condition cond = new Condition(Role.class);
        cond.createCriteria().andEqualTo("name", name).andEqualTo("deleted", false);
        return roleMapper.selectOneByExample(cond);
    }

    private TeacherResponse toResponse(Teacher t) {
        if (t == null) {
            return null;
        }
        User u = t.getUserId() != null ? userMapper.selectByPrimaryKey(t.getUserId()) : null;
        return TeacherResponse.builder()
                .id(t.getId()).userId(t.getUserId()).nip(t.getNip()).fullname(t.getFullname())
                .gender(t.getGender()).birthDate(t.getBirthDate()).address(t.getAddress())
                .phone(t.getPhone()).email(t.getEmail()).hireDate(t.getHireDate())
                .username(u != null ? u.getUsername() : null)
                .role("TEACHER")
                .build();
    }
}