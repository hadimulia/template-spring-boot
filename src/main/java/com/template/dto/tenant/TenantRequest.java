package com.template.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantRequest {
    private Long id;

    @NotBlank(message = "{validation.tenant.code.required}")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "{validation.tenant.code.invalid}")
    @Size(max = 50, message = "{validation.tenant.code.max}")
    private String code;

    @NotBlank(message = "{validation.tenant.name.required}")
    @Size(max = 100, message = "{validation.tenant.name.max}")
    private String name;

    private String description;

    private String status = "ACTIVE";
}
