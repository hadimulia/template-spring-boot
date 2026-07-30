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
import org.springframework.transaction.annotation.Transactional;

import com.template.entity.permission.Permission;
import com.template.entity.role.Role;
import com.template.entity.user.User;
import com.template.mapper.permission.PermissionMapper;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
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
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
