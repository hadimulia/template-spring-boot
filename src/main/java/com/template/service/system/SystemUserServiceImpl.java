package com.template.service.system;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

/**
 * Cross-school user management for the system realm. Each operation sets the
 * routing key to the chosen school's database BEFORE opening the transaction
 * (via TransactionTemplate), because a @Transactional proxy opens its connection
 * before the method body runs and would route with the stale key.
 */
@Service
public class SystemUserServiceImpl implements SystemUserService {

    private final SchoolMapper schoolMapper;
    private final SchoolUserMapper schoolUserMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;

    public SystemUserServiceImpl(SchoolMapper schoolMapper,
                                 SchoolUserMapper schoolUserMapper,
                                 UserMapper userMapper,
                                 UserRoleMapper userRoleMapper,
                                 RoleMapper roleMapper,
                                 PasswordEncoder passwordEncoder,
                                 PlatformTransactionManager transactionManager) {
        this.schoolMapper = schoolMapper;
        this.schoolUserMapper = schoolUserMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.tx = new TransactionTemplate(transactionManager);
    }

    private School requireActiveSchool(Long schoolId) {
        School school = schoolMapper.selectByPrimaryKey(schoolId);
        if (school == null || Boolean.TRUE.equals(school.getDeleted())
                || !"ACTIVE".equals(school.getStatus())) {
            throw new IllegalArgumentException("School not found or inactive: " + schoolId);
        }
        return school;
    }

    @Override
    public PageResult<UserResponse> listBySchool(Long schoolId, String keyword, int page, int size) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            return tx.execute(status -> {
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
            });
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public void create(Long schoolId, UserRequest request) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            tx.executeWithoutResult(status -> {
                if (userMapper.findByUsername(request.getUsername()) != null) {
                    throw new com.template.exception.BusinessException(
                            "Username already exists in this school: " + request.getUsername(),
                            "/system/users/new?schoolId=" + schoolId);
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
            });
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

    @Override
    public void update(Long schoolId, Long userId, UserUpdateRequest request) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            tx.executeWithoutResult(status -> {
                User user = userMapper.selectByPrimaryKey(userId);
                if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
                    throw new com.template.exception.BusinessException(
                            "User not found in school " + schoolId,
                            "/system/users?schoolId=" + schoolId);
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
            });
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public void delete(Long schoolId, Long userId) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            tx.executeWithoutResult(status -> {
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
            });
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public PageResult<UserResponse> listSystemUsers(String keyword, int page, int size) {
        return tx.execute(status -> {
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
        });
    }

    @Override
    public void createSystemUser(UserRequest request) {
        tx.executeWithoutResult(status -> {
            if (userMapper.findByUsername(request.getUsername()) != null) {
                throw new com.template.exception.BusinessException(
                        "Username already exists: " + request.getUsername(),
                        "/system/users/system/new");
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
        });
    }

    @Override
    public void updateSystemUser(Long userId, UserUpdateRequest request) {
        tx.executeWithoutResult(status -> {
            User user = userMapper.selectByPrimaryKey(userId);
            if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
                throw new com.template.exception.BusinessException(
                        "User not found", "/system/users/system");
            }
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
        });
    }

    @Override
    public void deleteSystemUser(Long userId) {
        tx.executeWithoutResult(status -> {
            User user = userMapper.selectByPrimaryKey(userId);
            if (user != null) {
                user.setDeleted(true);
                user.setUpdatedBy(SecurityUtils.getCurrentUsername());
                user.setUpdatedDate(LocalDateTime.now());
                userMapper.updateByPrimaryKey(user);
            }
        });
    }
}