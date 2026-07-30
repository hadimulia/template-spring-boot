package com.template.mapper.notification;

import com.template.dto.notification.NotificationResponse;
import com.template.entity.notification.Notification;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface NotificationMapper extends Mapper<Notification> {

    List<NotificationResponse> findByUserId(@Param("userId") Long userId,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    int countByUserId(@Param("userId") Long userId);

    int countUnreadByUserId(@Param("userId") Long userId);

    void markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    void markAllAsRead(@Param("userId") Long userId);
}
