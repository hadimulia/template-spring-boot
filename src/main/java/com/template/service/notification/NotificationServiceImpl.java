package com.template.service.notification;

import com.template.dto.PageResult;
import com.template.dto.notification.NotificationResponse;
import com.template.entity.notification.Notification;
import com.template.mapper.notification.NotificationMapper;
import com.template.service.generic.GenericServiceImpl;
import com.template.tenant.TenantContext;
import com.template.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl extends GenericServiceImpl<Notification, Long> implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        super(notificationMapper);
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void create(Long userId, String title, String message, String type, String link) {
        Notification notification = new Notification();
        notification.setTenantId(TenantContext.getTenantId());
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type != null ? type : "INFO");
        notification.setIsRead(false);
        notification.setLink(link);
        notification.setCreatedBy(SecurityUtils.getCurrentUsername());
        notification.setCreatedDate(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<NotificationResponse> findByCurrentUser(int page, int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long tenantId = TenantContext.getTenantId();
        int offset = (page - 1) * size;
        List<NotificationResponse> data = notificationMapper.findByUserId(userId, tenantId, offset, size);
        int total = notificationMapper.countByUserId(userId, tenantId);
        return PageResult.of(data, total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public int countUnread() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationMapper.countUnreadByUserId(userId, TenantContext.getTenantId());
    }

    @Override
    public void markAsRead(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationMapper.markAsRead(id, userId, TenantContext.getTenantId());
    }

    @Override
    public void markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationMapper.markAllAsRead(userId, TenantContext.getTenantId());
    }
}
