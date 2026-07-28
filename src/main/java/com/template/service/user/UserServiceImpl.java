package com.template.service.user;

import com.template.dto.PageResult;
import com.template.dto.UserRequest;
import com.template.dto.UserResponse;
import com.template.dto.UserUpdateRequest;
import com.template.entity.User;
import com.template.entity.UserRole;
import com.template.mapper.RoleMapper;
import com.template.mapper.UserMapper;
import com.template.mapper.UserRoleMapper;
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

    public UserServiceImpl(UserMapper userMapper,
                           UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder) {
        super(userMapper);
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
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

    public void create(UserRequest request) {
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
    }

    public void update(Long id, UserUpdateRequest request) {
        User user = get(id);
        if (user != null) {
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
        }
    }

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
