package com.template.service.approval;

import com.template.dto.PageResult;
import com.template.dto.approval.ApprovalRequestResponse;

import java.util.List;

public interface ApprovalService {
    PageResult<ApprovalRequestResponse> findAll(String keyword, int page, int size);
    List<ApprovalRequestResponse> findPending();
    void approve(Long id, String notes);
    void reject(Long id, String notes);
    Long submit(String entityType, Long entityId, String requestType, String requestData);
}
