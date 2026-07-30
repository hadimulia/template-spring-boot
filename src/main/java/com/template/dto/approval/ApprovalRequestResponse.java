package com.template.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private String requestType;
    private String status;
    private String requestData;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
}
