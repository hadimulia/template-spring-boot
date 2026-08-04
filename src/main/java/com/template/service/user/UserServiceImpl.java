package com.template.service.user;

import com.template.dto.PageResult;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;
import com.template.entity.registry.SchoolUser;
import com.template.entity.user.User;
import com.template.entity.user.UserRole;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;
import com.template.mapper.user.UserRoleMapper;
import com.template.registry.mapper.SchoolUserMapper;
import com.template.service.audit.Auditable;
import com.template.service.generic.GenericServiceImpl;
import com.template.util.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl extends GenericServiceImpl<User, Long> implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final SchoolUserMapper schoolUserMapper;

    public UserServiceImpl(UserMapper userMapper,
                           UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder,
                           SchoolUserMapper schoolUserMapper) {
        super(userMapper);
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.schoolUserMapper = schoolUserMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<UserResponse> findAll(String keyword, int page, int size) {
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
    }

    @Auditable(action = "CREATE", entityType = "USER", description = "#request.username")
    public void create(UserRequest request) {
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

        createUserIndex(user);
    }

    private void createUserIndex(User user) {
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        if (schoolId == null) {
            throw new IllegalStateException("No school context for user creation");
        }
        SchoolUser index = new SchoolUser();
        index.setSchoolId(schoolId);
        index.setUserId(user.getId());
        index.setUsername(user.getUsername());
        index.setEnabled(true);
        index.setCreatedBy(SecurityUtils.getCurrentUsername());
        index.setDeleted(false);
        schoolUserMapper.insertSelective(index);
    }

    @Auditable(action = "UPDATE", entityType = "USER", description = "#request.username")
    public void update(Long id, UserUpdateRequest request) {
        User user = get(id);
        if (user != null) {
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
                List<Long> existingRoleIds = userRoleMapper.findRoleIdsByUserIdAll(id);
                for (Long roleId : existingRoleIds) {
                    UserRole ur = new UserRole();
                    ur.setUserId(id);
                    ur.setRoleId(roleId);
                    userRoleMapper.delete(ur);
                }

                for (Long roleId : request.getRoleIds()) {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(id);
                    userRole.setRoleId(roleId);
                    userRole.setCreatedBy(SecurityUtils.getCurrentUsername());
                    userRole.setCreatedDate(LocalDateTime.now());
                    userRoleMapper.insert(userRole);
                }
            }

            if (usernameChanged) {
                syncUserIndex(id, request.getUsername());
            }
        }
    }

    private void syncUserIndex(Long userId, String newUsername) {
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        if (schoolId == null) {
            return; // system realm has no per-school index to sync
        }
        SchoolUser index = schoolUserMapper.findByUserIdAndSchool(userId, schoolId);
        if (index != null) {
            index.setUsername(newUsername);
            index.setUpdatedBy(SecurityUtils.getCurrentUsername());
            index.setUpdatedDate(LocalDateTime.now());
            schoolUserMapper.updateByPrimaryKey(index);
        }
    }

    @Auditable(action = "DELETE", entityType = "USER", description = "")
    public void delete(Long id) {
        User user = get(id);
        if (user != null) {
            user.setDeleted(true);
            user.setUpdatedBy(SecurityUtils.getCurrentUsername());
            user.setUpdatedDate(LocalDateTime.now());
            userMapper.updateByPrimaryKey(user);
        }
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = get(id);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) return null;

        List<Long> roleIds = userRoleMapper.findRoleIdsByUserId(id);
        List<String> roleNames = roleIds.stream()
                .map(roleId -> roleMapper.selectByPrimaryKey(roleId))
                .filter(r -> r != null)
                .map(r -> r.getName())
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullname(user.getFullname())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .lastLogin(user.getLastLogin())
                .roles(roleNames)
                .createdBy(user.getCreatedBy())
                .createdDate(user.getCreatedDate())
                .updatedBy(user.getUpdatedBy())
                .updatedDate(user.getUpdatedDate())
                .build();
    }
}
