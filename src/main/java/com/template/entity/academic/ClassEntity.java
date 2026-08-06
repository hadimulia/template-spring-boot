package com.template.entity.academic;

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
@Table(name = "classes")
public class ClassEntity extends BaseEntity {
    private String name;
    private String grade;
    private String academicYear;
    private Long homeroomTeacherId;
}