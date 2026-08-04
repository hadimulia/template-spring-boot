package com.template.entity.approval;

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
@Table(name = "approval_requests")
public class ApprovalRequest {
    @Id
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
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
