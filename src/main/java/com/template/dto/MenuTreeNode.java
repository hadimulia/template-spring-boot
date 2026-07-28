package com.template.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class MenuTreeNode {
    private Long id;
    private String name;
    private String url;
    private String icon;
    private Integer sortOrder;
    private Long parentId;
    private boolean active;
    private List<MenuTreeNode> children;
}
