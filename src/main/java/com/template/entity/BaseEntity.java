package com.template.entity;

import java.time.LocalDateTime;

import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import tk.mybatis.mapper.annotation.KeySql;

@Getter
@Setter
public abstract class BaseEntity {
    
	@Id
    @KeySql(genId = PostgreSqlSequenceGenId.class)
    private Long id;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
    private Boolean deleted = false;
    private Integer version = 0;
}
