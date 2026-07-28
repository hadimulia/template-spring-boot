package com.template.entity;

import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "menus")
public class Menu extends BaseEntity {
    private Long parentId;
    private String name;
    private String url;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
}
