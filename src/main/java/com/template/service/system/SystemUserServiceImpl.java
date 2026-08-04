package com.template.service.system;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.dto.PageResult;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;
import com.template.entity.registry.SchoolUser;
import com.template.entity.school.School;
import com.template.entity.user.User;
import com.template.entity.user.UserRole;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;
import com.template.mapper.user.UserRoleMapper;
import com.template.registry.mapper.SchoolMapper;
import com.template.registry.mapper.SchoolUserMapper;
import com.template.tenant.TenantContext;
import com.template.util.SecurityUtils;

@Service
public class SystemUserServiceImpl implements SystemUserService {

    private final SchoolMapper schoolMapper;
    private final SchoolUserMapper schoolUserMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public SystemUserServiceImpl(SchoolMapper schoolMapper,
                                 SchoolUserMapper schoolUserMapper,
                                 UserMapper userMapper,
                                 UserRoleMapper userRoleMapper,
                                 RoleMapper roleMapper,
                                 PasswordEncoder passwordEncoder) {
        this.schoolMapper = schoolMapper;
        this.schoolUserMapper = schoolUserMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    private School requireActiveSchool(Long schoolId) {
        School school = schoolMapper.selectByPrimaryKey(schoolId);
        if (school == null || Boolean.TRUE.equals(school.getDeleted())
                || !"ACTIVE".equals(school.getStatus())) {
            throw new IllegalArgumentException("School not found or inactive: " + schoolId);
        }
        return school;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<UserResponse> listBySchool(Long schoolId, String keyword, int page, int size) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            int offset = (page - 1) * size;
            List<UserResponse> data = userMapper.findAll(keyword, offset, size);
            int total = userMapper.countAll(keyword);
            for (UserResponse user : data) {
                List<Long> roleIds = userRoleMapper.findRoleIdsByUserId(user.getId());
                List<String> roleNames = roleIds.stream()
                        .map(roleId -> roleMapper.selectByPrimaryKey(roleId))
                        .filter(r -> r != null && !Boolean.TRUE.equals(r.getDeleted()))
                        .map(r -> r.getName())
                        .collect(Collectors.toList());
                user.setRoles(roleNames);
            }
            return PageResult.of(data, total, page, size);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    @Override
    public void create(Long schoolId, UserRequest request) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            if (userMapper.findByUsername(request.getUsername()) != null) {
                throw new IllegalArgumentException("Username already exists in this school: " + request.getUsername());
            }
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFullname(request.getFullname());
            user.setEmail(request.getEmail());
            user.setEnabled(true);
            user.setAccountLocked(false);
            user.setLoginAttempts(0);
            user.setCreatedBy(SecurityUtils.getCurrentUsername());
            user.setCreatedDate(LocalDateTime.now());
            user.setDeleted(false);
            user.setVersion(0);
            userMapper.insert(user);

            if (request.getRoleIds() != null) {
                for (Long roleId : request.getRoleIds()) {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(user.getId());
                    userRole.setRoleId(roleId);
                    userRole.setCreatedBy(SecurityUtils.getCurrentUsername());
                    userRole.setCreatedDate(LocalDateTime.now());
                    userRoleMapper.insert(userRole);
                }
            }

            createUserIndex(user, schoolId);
        } finally {
            TenantContext.clear();
        }
    }

    private void createUserIndex(User user, Long schoolId) {
        SchoolUser index = new SchoolUser();
        index.setSchoolId(schoolId);
        index.setUserId(user.getId());
        index.setUsername(user.getUsername());
        index.setEnabled(true);
        index.setCreatedBy(SecurityUtils.getCurrentUsername());
        index.setDeleted(false);
        schoolUserMapper.insertSelective(index);
    }

    @Transactional
    @Override
    public void update(Long schoolId, Long userId, UserUpdateRequest request) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            User user = userMapper.selectByPrimaryKey(userId);
            if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
                throw new IllegalArgumentException("User not found in school " + schoolId);
            }
            boolean usernameChanged = request.getUsername() != null
                    && !request.getUsername().equals(user.getUsername());
            user.setUsername(request.getUsername());
            user.setFullname(request.getFullname());
            user.setEmail(request.getEmail());
            if (request.getEnabled() != null) {
                user.setEnabled(request.getEnabled());
            }
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            user.setUpdatedBy(SecurityUtils.getCurrentUsername());
            user.setUpdatedDate(LocalDateTime.now());
            userMapper.updateByPrimaryKey(user);

            if (request.getRoleIds() != null) {
                List<Long> existingRoleIds = userRoleMapper.findRoleIdsByUserIdAll(userId);
                for (Long roleId : existingRoleIds) {
                    UserRole ur = new UserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    userRoleMapper.delete(ur);
                }
                for (Long roleId : request.getRoleIds()) {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRole.setCreatedBy(SecurityUtils.getCurrentUsername());
                    userRole.setCreatedDate(LocalDateTime.now());
                    userRoleMapper.insert(userRole);
                }
            }

            if (usernameChanged) {
                SchoolUser index = schoolUserMapper.findByUserIdAndSchool(userId, schoolId);
                if (index != null) {
                    index.setUsername(request.getUsername());
                    index.setUpdatedBy(SecurityUtils.getCurrentUsername());
                    index.setUpdatedDate(LocalDateTime.now());
                    schoolUserMapper.updateByPrimaryKey(index);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    @Override
    public void delete(Long schoolId, Long userId) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            User user = userMapper.selectByPrimaryKey(userId);
            if (user != null) {
                user.setDeleted(true);
                user.setUpdatedBy(SecurityUtils.getCurrentUsername());
                user.setUpdatedDate(LocalDateTime.now());
                userMapper.updateByPrimaryKey(user);

                SchoolUser index = schoolUserMapper.findByUserIdAndSchool(userId, schoolId);
                if (index != null) {
                    index.setDeleted(true);
                    index.setUpdatedBy(SecurityUtils.getCurrentUsername());
                    index.setUpdatedDate(LocalDateTime.now());
                    schoolUserMapper.updateByPrimaryKey(index);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }
}