package com.template.entity.tenant;

import java.time.LocalDateTime;

import javax.persistence.Id;
import javax.persistence.Table;

import com.template.entity.PostgreSqlSequenceGenId;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tk.mybatis.mapper.annotation.KeySql;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Table(name = "tenants")
public class Tenant {
    @Id
    @KeySql(genId = PostgreSqlSequenceGenId.class)
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
    private Boolean deleted;
    private Integer version;
}
