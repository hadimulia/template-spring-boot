package com.template.entity;

import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "user_roles")
public class UserRole extends BaseEntity {
    private Long userId;
    private Long roleId;
}
