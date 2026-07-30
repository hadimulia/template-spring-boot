package com.template.controller.notification;

import com.template.dto.PageResult;
import com.template.dto.notification.NotificationResponse;
import com.template.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<NotificationResponse> result = notificationService.findByCurrentUser(page, size);
        model.addAttribute("notifications", result.getData());
        model.addAttribute("pagination", result.getPagination());
        return "notification/list";
    }

    @GetMapping("/api/unread-count")
    @ResponseBody
    public Map<String, Object> unreadCount() {
        int count = notificationService.countUnread();
        return Map.of("count", count);
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public Map<String, Object> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Map.of("success", true);
    }

    @PostMapping("/read-all")
    @ResponseBody
    public Map<String, Object> markAllRead() {
        notificationService.markAllAsRead();
        return Map.of("success", true);
    }
}
