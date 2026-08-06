package com.template.dto.academic;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentRequest {
    private Long id;

    @NotBlank(message = "NIS is required")
    private String nis;

    @NotBlank(message = "Full name is required")
    private String fullname;

    private String gender;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private String enrollmentStatus = "ACTIVE";
    private Long classId;

    /** Initial password for the auto-created login account. */
    @NotBlank(message = "Password is required")
    private String password;
}