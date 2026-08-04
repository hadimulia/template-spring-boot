package com.template.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {

    private final Long userId;
    private final Long schoolId;
    private final String schoolCode;
    private final String schoolDbName;

    public CustomUserDetails(Long userId, Long schoolId, String schoolCode, String schoolDbName,
                             String username, String password,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
        this.schoolId = schoolId;
        this.schoolCode = schoolCode;
        this.schoolDbName = schoolDbName;
    }
}
