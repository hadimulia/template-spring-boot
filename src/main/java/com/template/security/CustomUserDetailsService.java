package com.template.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;

import com.template.config.SchoolDataSourceManager;
import com.template.config.SystemDataSourceManager;
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
 * Authenticates across two realms, chosen by the login form's school code:
 * <ol>
 *   <li>System realm (code {@code system}): loads the master admin from
 *       {@code sims_system} to control all schools.</li>
 *   <li>School realm (any other code): resolves the school in the registry,
 *       looks up the user index ({@code school_users}) by school + username,
 *       then loads credentials/RBAC from that school's database (auto-provisioning
 *       it on first login via {@link SchoolDataSourceManager}).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String SYSTEM_CODE = "system";
    private static final String SYSTEM_DB_NAME = "sims_system";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final SchoolUserMapper schoolUserMapper;
    private final SchoolMapper schoolMapper;
    private final SchoolDataSourceManager schoolDataSourceManager;
    private final SystemDataSourceManager systemDataSourceManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String schoolCode = currentSchoolCode();
        if (SYSTEM_CODE.equalsIgnoreCase(schoolCode)) {
            return loadSystemUser(username);
        }
        return loadSchoolUser(username, schoolCode);
    }

    private String currentSchoolCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken token
                && token.getDetails() instanceof WebAuthenticationDetails details) {
            if (details instanceof SchoolCodeWebAuthenticationDetails sc) {
                String code = sc.getSchoolCode();
                if (code != null && !code.isBlank()) {
                    return code.trim();
                }
            }
        }
        throw new UsernameNotFoundException("School code is required");
    }

    private UserDetails loadSchoolUser(String username, String schoolCode) {
        School school = schoolMapper.findByCode(schoolCode);
        if (school == null || Boolean.TRUE.equals(school.getDeleted())
                || !"ACTIVE".equals(school.getStatus())) {
            throw new UsernameNotFoundException("School not found or inactive: " + schoolCode);
        }

        SchoolUser index = schoolUserMapper.findBySchoolAndUsername(school.getId(), username);
        if (index == null) {
            throw new UsernameNotFoundException("User not found in this school");
        }
        if (!Boolean.TRUE.equals(index.getEnabled())) {
            throw new DisabledException("Account is disabled");
        }

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

            return buildDetails(user.getId(), school.getId(), school.getCode(),
                    school.getDbName(), user);
        } finally {
            TenantContext.clear();
        }
    }

    private UserDetails loadSystemUser(String username) {
        TenantContext.setRoutingKey(SYSTEM_CODE);
        try {
            systemDataSourceManager.getOrCreate();
            User user = userMapper.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
            if (!user.getEnabled()) {
                throw new DisabledException("Account is disabled");
            }
            if (user.getAccountLocked()) {
                throw new LockedException("Account is locked");
            }

            return buildDetails(user.getId(), null, SYSTEM_CODE, SYSTEM_DB_NAME, user);
        } finally {
            TenantContext.clear();
        }
    }

    private UserDetails buildDetails(Long userId, Long schoolId, String schoolCode,
                                     String dbName, User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        List<Role> roles = roleMapper.findByUserId(user.getId());
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            List<Permission> permissions = permissionMapper.findByRoleId(role.getId());
            for (Permission permission : permissions) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }
        return new CustomUserDetails(userId, schoolId, schoolCode, dbName,
                user.getUsername(), user.getPassword(), authorities);
    }
}