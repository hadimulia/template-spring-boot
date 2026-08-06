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
@Table(name = "class_students")
public class ClassStudent extends BaseEntity {
    private Long classId;
    private Long studentId;
}