package com.template.entity;

import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "roles")
public class Role extends BaseEntity {
    private String name;
    private String description;
}
