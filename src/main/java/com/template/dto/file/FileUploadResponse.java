package com.template.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long id;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String entityType;
    private Long entityId;
    private String createdBy;
    private LocalDateTime createdDate;
}
