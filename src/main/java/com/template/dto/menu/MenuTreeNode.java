package com.template.dto.menu;

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
    private String i18nKey;
    private List<MenuTreeNode> children;
}
