package com.template.entity;

import javax.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "role_permissions")
public class RolePermission extends BaseEntity {
    private Long roleId;
    private Long permissionId;
}
