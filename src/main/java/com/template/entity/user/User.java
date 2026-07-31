package com.template.entity.user;

import java.time.LocalDateTime;

import javax.persistence.Table;

import com.template.entity.BaseEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "users")
public class User extends BaseEntity {
    private Long tenantId;
    private String username;
    private String password;
    private String fullname;
    private String email;
    private Boolean enabled;
    private Boolean accountLocked;
    private Integer loginAttempts;
    private LocalDateTime lastLogin;
}
