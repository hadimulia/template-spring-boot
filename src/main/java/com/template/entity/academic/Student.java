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
@Table(name = "students")
public class Student extends BaseEntity {
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
}