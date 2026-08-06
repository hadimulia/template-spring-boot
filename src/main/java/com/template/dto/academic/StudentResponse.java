package com.template.dto.academic;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentResponse {
    private Long id;
    private Long userId;
    private String nis;
    private String fullname;
    private String gender;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private String enrollmentStatus;
    private Long classId;
    private String username;
    private String role;
}