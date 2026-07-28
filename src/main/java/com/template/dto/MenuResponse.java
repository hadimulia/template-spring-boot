package com.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.template.entity.Menu;
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
        response.setCreatedBy(SecurityUtils.getCurrentUsername());
        response.setCreatedDate(LocalDateTime.now());
        response.setVisible(menu.getVisible());
    	return response;
    }
}
