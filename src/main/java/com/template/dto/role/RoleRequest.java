package com.template.dto.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class RoleRequest {
    private Long id;

    @NotBlank(message = "{validation.role.name.required}")
    private String name;

    private String description;

    private List<Long> permissionIds;

    private List<Long> menuIds;
}
