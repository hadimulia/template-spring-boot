package com.template.entity;

import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "role_menus")
public class RoleMenu extends BaseEntity {
    private Long roleId;
    private Long menuId;
}
