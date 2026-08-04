package com.template.entity.registry;

import javax.persistence.Id;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tk.mybatis.mapper.annotation.KeySql;

import java.time.LocalDateTime;

/**
 * Global login index in the registry database. Maps a globally-unique username
 * to a school ({@code school_id}) and the matching user row inside that school's
 * database ({@code user_id}). Credentials and RBAC stay in the school DB.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Table(name = "school_users")
public class SchoolUser {
    @Id
    @KeySql(genId = RegistrySequenceGenId.class)
    private Long id;
    private Long schoolId;
    private Long userId;
    private String username;
    private Boolean enabled;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
    private Boolean deleted;
}
