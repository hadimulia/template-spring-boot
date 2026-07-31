package com.template.dto.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermissionRequest {
    private Long id;

    @NotBlank(message = "{validation.permission.code.required}")
    private String code;

    @NotBlank(message = "{validation.permission.description.required}")
    private String description;
}
