// src/main/java/com/azedcods/home_buddy_v2/service/notification/NotificationSpecs.java
package com.azedcods.home_buddy_v2.service.notification;

import com.azedcods.home_buddy_v2.enums.NotificationType;
import com.azedcods.home_buddy_v2.model.notif.Notification;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class NotificationSpecs {

    private NotificationSpecs() {}

    public static Specification<Notification> recipient(Long userId) {
        if (userId == null) {
            return (root, query, cb) -> cb.conjunction(); // ALL users
        }
        return (root, query, cb) -> cb.equal(root.get("recipientUser").get("userId"), userId);
    }

    public static Specification<Notification> unreadOnly(Boolean unreadOnly) {
        if (unreadOnly == null || !unreadOnly) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.isNull(root.get("readAt"));
    }

    public static Specification<Notification> type(NotificationType type) {
        if (type == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Notification> from(Instant from) {
        if (from == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Notification> to(Instant to) {
        if (to == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<Notification> search(String q) {
        if (q == null || q.trim().isEmpty()) return (root, query, cb) -> cb.conjunction();
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("message")), like)
        );
    }
}
