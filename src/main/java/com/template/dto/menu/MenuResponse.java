package com.template.dto.menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.template.entity.menu.Menu;
import com.template.util.SecurityUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponse {
    private Long id;
    private Long parentId;
    private String parentName;
    private String name;
    private String url;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
    private String i18nKey;
    private String parentI18nKey;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;

    public static MenuResponse of(Menu menu) {
        MenuResponse response = new MenuResponse();
        response.setParentId(menu.getParentId());
        response.setName(menu.getName());
        response.setUrl(menu.getUrl());
        response.setIcon(menu.getIcon());
        response.setSortOrder(menu.getSortOrder());
        response.setVisible(menu.getVisible());
        response.setI18nKey(menu.getI18nKey());
        response.setCreatedBy(SecurityUtils.getCurrentUsername());
        response.setCreatedDate(LocalDateTime.now());
        response.setVisible(menu.getVisible());
        return response;
    }
}
