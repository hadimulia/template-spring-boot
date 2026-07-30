package com.template.entity.file;

import java.time.LocalDateTime;

import javax.persistence.Id;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Table(name = "file_uploads")
public class FileUpload {
    @Id
    private Long id;
    private String originalName;
    private String storedName;
    private String contentType;
    private Long fileSize;
    private String entityType;
    private Long entityId;
    private String createdBy;
    private LocalDateTime createdDate;
}
