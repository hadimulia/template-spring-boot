package com.template.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MenuRequest {
    private Long id;
    private Long parentId;

    @NotBlank(message = "{validation.menu.name.required}")
    private String name;

    private String url;

    private String icon;

    private Integer sortOrder;

    private Boolean visible = true;

    private String i18nKey;
}
