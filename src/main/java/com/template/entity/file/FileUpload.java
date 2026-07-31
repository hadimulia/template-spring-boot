package com.template.entity.file;

import com.template.entity.BaseEntity;

import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "file_uploads")
public class FileUpload extends BaseEntity {
    private Long tenantId;
    private String originalName;
    private String storedName;
    private String contentType;
    private Long fileSize;
    private String entityType;
    private Long entityId;
}
