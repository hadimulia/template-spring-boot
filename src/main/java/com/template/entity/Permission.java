package com.template.entity;

import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "permissions")
public class Permission extends BaseEntity {
    private String code;
    private String description;
}
