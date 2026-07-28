package com.template.entity;

import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "users")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String fullname;
    private String email;
    private Boolean enabled;
    private Boolean accountLocked;
    private Integer loginAttempts;
    private LocalDateTime lastLogin;
}
