package com.template.dto.academic;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassResponse {
    private Long id;
    private String name;
    private String grade;
    private String academicYear;
    private Long homeroomTeacherId;
    private String homeroomTeacherName;
    private List<Long> studentIds;
    private int studentCount;
}