package com.template.entity.academic;

import java.time.LocalDate;

import javax.persistence.Table;

import com.template.entity.BaseEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "teachers")
public class Teacher extends BaseEntity {
    private Long userId;
    private String nip;
    private String fullname;
    private String gender;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private LocalDate hireDate;
}