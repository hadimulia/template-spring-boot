package com.template.dto.academic;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassRequest {
    private Long id;

    @NotBlank(message = "Class name is required")
    private String name;

    private String grade;
    private String academicYear;
    private Long homeroomTeacherId;
    private List<Long> studentIds;
}