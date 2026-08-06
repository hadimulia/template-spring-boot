package com.template.dto.academic;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeacherRequest {
    private Long id;

    @NotBlank(message = "NIP is required")
    private String nip;

    @NotBlank(message = "Full name is required")
    private String fullname;

    private String gender;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private LocalDate hireDate;

    /** Initial password for the auto-created login account. */
    @NotBlank(message = "Password is required")
    private String password;
}