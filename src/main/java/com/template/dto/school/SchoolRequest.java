package com.template.dto.school;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SchoolRequest {
    private Long id;

    @NotBlank(message = "{validation.school.code.required}")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "{validation.school.code.invalid}")
    @Size(max = 50, message = "{validation.school.code.max}")
    private String code;

    @NotBlank(message = "{validation.school.name.required}")
    @Size(max = 100, message = "{validation.school.name.max}")
    private String name;

    private String description;

    private String status = "ACTIVE";
}
