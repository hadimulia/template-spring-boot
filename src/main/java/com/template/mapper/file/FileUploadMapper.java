package com.template.mapper.file;

import com.template.dto.file.FileUploadResponse;
import com.template.entity.file.FileUpload;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface FileUploadMapper extends Mapper<FileUpload> {
    List<FileUploadResponse> findByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId, @Param("tenantId") Long tenantId);

    List<FileUploadResponse> findAll(@Param("tenantId") Long tenantId);
}
