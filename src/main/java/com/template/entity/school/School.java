package com.template.entity.school;

import java.time.LocalDateTime;

import javax.persistence.Id;
import javax.persistence.Table;

import com.template.entity.registry.RegistrySequenceGenId;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * A school (tenant) in the registry database. Each school owns its own database
 * {@code sims_<code>} created on onboarding via {@link com.template.config.SchoolDataSourceManager}.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Table(name = "schools")
public class School {
    @Id
    @KeySql(genId = RegistrySequenceGenId.class)
    private Long id;
    private String code;
    private String name;
    private String dbName;
    private String description;
    private String status;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
    private Boolean deleted;
    private Integer version;
}
