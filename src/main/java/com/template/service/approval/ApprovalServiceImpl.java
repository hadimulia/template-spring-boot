package com.template.service.approval;

import com.template.dto.PageResult;
import com.template.dto.approval.ApprovalRequestResponse;
import com.template.entity.approval.ApprovalRequest;
import com.template.mapper.approval.ApprovalRequestMapper;
import com.template.tenant.TenantContext;
import com.template.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRequestMapper approvalRequestMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ApprovalRequestResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        Long tenantId = TenantContext.getTenantId();
        List<ApprovalRequestResponse> data = approvalRequestMapper.findPage(keyword, tenantId, offset, size);
        int total = approvalRequestMapper.countPage(keyword, tenantId);
        return PageResult.of(data, total, page, size);
    }

    @Override
    public List<ApprovalRequestResponse> findPending() {
        return approvalRequestMapper.findPending(TenantContext.getTenantId());
    }

    @Override
    @Transactional
    public void approve(Long id, String notes) {
        ApprovalRequest req = approvalRequestMapper.selectByPrimaryKey(id);
        if (req != null && "PENDING".equals(req.getStatus())) {
            req.setStatus("APPROVED");
            req.setReviewedBy(SecurityUtils.getCurrentUsername());
            req.setReviewedAt(LocalDateTime.now());
            req.setReviewNotes(notes);
            req.setUpdatedBy(SecurityUtils.getCurrentUsername());
            req.setUpdatedDate(LocalDateTime.now());
            approvalRequestMapper.updateByPrimaryKey(req);
        }
    }

    @Override
    @Transactional
    public void reject(Long id, String notes) {
        ApprovalRequest req = approvalRequestMapper.selectByPrimaryKey(id);
        if (req != null && "PENDING".equals(req.getStatus())) {
            req.setStatus("REJECTED");
            req.setReviewedBy(SecurityUtils.getCurrentUsername());
            req.setReviewedAt(LocalDateTime.now());
            req.setReviewNotes(notes);
            req.setUpdatedBy(SecurityUtils.getCurrentUsername());
            req.setUpdatedDate(LocalDateTime.now());
            approvalRequestMapper.updateByPrimaryKey(req);
        }
    }

    @Override
    @Transactional
    public Long submit(String entityType, Long entityId, String requestType, String requestData) {
        ApprovalRequest req = new ApprovalRequest();
        req.setTenantId(TenantContext.getTenantId());
        req.setEntityType(entityType);
        req.setEntityId(entityId);
        req.setRequestType(requestType);
        req.setStatus("PENDING");
        req.setRequestData(requestData);
        req.setSubmittedBy(SecurityUtils.getCurrentUsername());
        req.setSubmittedAt(LocalDateTime.now());
        req.setCreatedBy(SecurityUtils.getCurrentUsername());
        req.setCreatedDate(LocalDateTime.now());
        approvalRequestMapper.insert(req);
        return req.getId();
    }
}
