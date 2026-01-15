// src/main/java/com/azedcods/home_buddy_v2/controller/NotificationController.java
package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.enums.NotificationType;
import com.azedcods.home_buddy_v2.payload.NotificationDtos;
import com.azedcods.home_buddy_v2.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // Admin: userId omitted => ALL users
    // Non-admin: you should still pass userId (RBAC can enforce).
    @GetMapping
    public Page<NotificationDtos.Response> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.list(userId, unreadOnly, type, from, to, q, page, size);
    }

    // Admin: userId omitted => unread count across ALL users
    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCountResponse unreadCount(@RequestParam(required = false) Long userId) {
        return notificationService.unreadCount(userId);
    }

    @PostMapping
    public NotificationDtos.Response create(@RequestBody NotificationDtos.CreateRequest req) {
        return notificationService.create(req);
    }

    @PostMapping("/{id}/read")
    public NotificationDtos.Response markRead(@PathVariable Long id, @RequestParam Long userId) {
        return notificationService.markRead(id, userId);
    }

    @PostMapping("/read-all")
    public int markAllRead(@RequestParam Long userId) {
        return notificationService.markAllRead(userId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        notificationService.delete(id);
    }
}
