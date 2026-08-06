package com.template.dto.academic;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherResponse {
    private Long id;
    private Long userId;
    private String nip;
    private String fullname;
    private String gender;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private LocalDate hireDate;
    private String username;
    private String role;
}