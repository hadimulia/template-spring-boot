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
@Table(name = "menus")
public class Menu extends BaseEntity {
    private Long parentId;
    private String name;
    private String url;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
}
