package com.template.service.notification;

import com.template.dto.PageResult;
import com.template.dto.notification.NotificationResponse;

public interface NotificationService {

    void create(Long userId, String title, String message, String type, String link);

    PageResult<NotificationResponse> findByCurrentUser(int page, int size);

    int countUnread();

    void markAsRead(Long id);

    void markAllAsRead();
}
