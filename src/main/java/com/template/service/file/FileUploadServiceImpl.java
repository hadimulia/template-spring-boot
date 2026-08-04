package com.template.service.file;

import com.template.dto.file.FileUploadResponse;
import com.template.entity.file.FileUpload;
import com.template.mapper.file.FileUploadMapper;
import com.template.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileUploadMapper fileUploadMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public FileUploadResponse upload(MultipartFile file, String entityType, Long entityId) {
        try {
            String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetDir = Path.of(uploadDir);
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(storedName));

            FileUpload entity = new FileUpload();
            entity.setOriginalName(file.getOriginalFilename());
            entity.setStoredName(storedName);
            entity.setContentType(file.getContentType());
            entity.setFileSize(file.getSize());
            entity.setEntityType(entityType);
            entity.setEntityId(entityId);
            entity.setCreatedBy(SecurityUtils.getCurrentUsername());
            entity.setCreatedDate(LocalDateTime.now());
            fileUploadMapper.insert(entity);

            return toResponse(entity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileUploadResponse> findByEntity(String entityType, Long entityId) {
        return fileUploadMapper.findByEntity(entityType, entityId);
    }

    @Override
    public List<FileUploadResponse> findAll() {
        return fileUploadMapper.findAll();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FileUpload entity = fileUploadMapper.selectByPrimaryKey(id);
        if (entity != null) {
            try {
                Files.deleteIfExists(Path.of(uploadDir).resolve(entity.getStoredName()));
            } catch (IOException ignored) {}
            fileUploadMapper.deleteByPrimaryKey(id);
        }
    }

    private FileUploadResponse toResponse(FileUpload entity) {
        return FileUploadResponse.builder()
                .id(entity.getId())
                .originalName(entity.getOriginalName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .build();
    }
}
