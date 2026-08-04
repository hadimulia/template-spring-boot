package com.template.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.template.config.SchoolDataSourceManager;
import com.template.entity.permission.Permission;
import com.template.entity.registry.SchoolUser;
import com.template.entity.role.Role;
import com.template.entity.school.School;
import com.template.entity.user.User;
import com.template.mapper.permission.PermissionMapper;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;
import com.template.registry.mapper.SchoolMapper;
import com.template.registry.mapper.SchoolUserMapper;
import com.template.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

/**
 * Authenticates across two realms:
 * <ol>
 *   <li>Registry realm: resolves the username in {@code school_users} and reads
 *       the school ({@code schools}) to get its database name.</li>
 *   <li>School realm: loads credentials, roles and permissions from that school's
 *       database (auto-provisioning it on first login via {@link SchoolDataSourceManager}).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final SchoolUserMapper schoolUserMapper;
    private final SchoolMapper schoolMapper;
    private final SchoolDataSourceManager schoolDataSourceManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SchoolUser index = schoolUserMapper.findByUsername(username);
        if (index == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        if (!Boolean.TRUE.equals(index.getEnabled())) {
            throw new DisabledException("Account is disabled");
        }

        School school = schoolMapper.selectByPrimaryKey(index.getSchoolId());
        if (school == null || Boolean.TRUE.equals(school.getDeleted())
                || !"ACTIVE".equals(school.getStatus())) {
            throw new UsernameNotFoundException("School not found or inactive for username: " + username);
        }

        // Route to the school database and (re)load credentials/RBAC from it.
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(school.getId());
        try {
            schoolDataSourceManager.getOrCreate(school.getCode());

            User user = userMapper.selectByPrimaryKey(index.getUserId());
            if (user == null) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
            if (!user.getEnabled()) {
                throw new DisabledException("Account is disabled");
            }
            if (user.getAccountLocked()) {
                throw new LockedException("Account is locked");
            }

            List<Role> roles = roleMapper.findByUserId(user.getId());

            Set<GrantedAuthority> authorities = new HashSet<>();
            for (Role role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

                List<Permission> permissions = permissionMapper.findByRoleId(role.getId());
                for (Permission permission : permissions) {
                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                }
            }

            return new CustomUserDetails(
                    user.getId(),
                    school.getId(),
                    school.getCode(),
                    school.getDbName(),
                    user.getUsername(),
                    user.getPassword(),
                    authorities
            );
        } finally {
            TenantContext.clear();
        }
    }
}
