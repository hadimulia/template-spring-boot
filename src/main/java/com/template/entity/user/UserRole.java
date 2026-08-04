package com.template.entity.user;

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
@Table(name = "user_roles")
public class UserRole extends BaseEntity {
    private Long userId;
    private Long roleId;
}
