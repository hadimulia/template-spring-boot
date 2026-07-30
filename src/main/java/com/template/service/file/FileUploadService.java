package com.template.service.file;

import com.template.dto.file.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileUploadService {
    FileUploadResponse upload(MultipartFile file, String entityType, Long entityId);
    List<FileUploadResponse> findByEntity(String entityType, Long entityId);
    List<FileUploadResponse> findAll();
    void delete(Long id);
}
