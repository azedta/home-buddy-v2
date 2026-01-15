package com.azedcods.home_buddy_v2.payload;

import com.azedcods.home_buddy_v2.enums.NotificationRule;
import com.azedcods.home_buddy_v2.enums.NotificationSeverity;
import com.azedcods.home_buddy_v2.enums.NotificationType;

import java.time.Instant;

public class NotificationDtos {

    public record CreateRequest(
            Long recipientUserId,
            Long actorUserId,
            NotificationRule rule,
            String notificationKey,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String sourceModule,
            String actionUrl,
            String relatedEntityType,
            String relatedEntityId
    ) {}

    public record Response(
            Long id,
            Long recipientUserId,
            Long actorUserId,
            NotificationRule rule,
            String notificationKey,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String sourceModule,
            String actionUrl,
            String relatedEntityType,
            String relatedEntityId,
            Instant createdAt,
            Instant readAt,
            boolean read
    ) {}

    public record UnreadCountResponse(Long userId, long unreadCount) {}
}
