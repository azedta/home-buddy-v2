package com.azedcods.home_buddy_v2.model.notif;

import com.azedcods.home_buddy_v2.enums.NotificationRule;
import com.azedcods.home_buddy_v2.enums.NotificationSeverity;
import com.azedcods.home_buddy_v2.enums.NotificationType;
import com.azedcods.home_buddy_v2.model.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_user_id, created_at"),
        @Index(name = "idx_notification_recipient_readat", columnList = "recipient_user_id, read_at"),
        @Index(name = "idx_notification_type", columnList = "type"),
        @Index(name = "idx_notification_rule", columnList = "rule"),
        @Index(name = "idx_notification_key", columnList = "notification_key")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // who sees it
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    // optional: who triggered it (admin/caregiver/system)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationSeverity severity;

    // NEW: rule + dedupe key
    @Enumerated(EnumType.STRING)
    @Column(name = "rule", nullable = false, length = 60)
    private NotificationRule rule;

    // e.g. "DOSE_MISSED:occ=123:user=4" or "ROBOT_DOWN:robot=LEO-1:user=4"
    @Column(name = "notification_key", length = 200)
    private String notificationKey;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "source_module", length = 60)
    private String sourceModule;

    @Column(name = "action_url", length = 300)
    private String actionUrl;

    @Column(name = "related_entity_type", length = 60)
    private String relatedEntityType;

    @Column(name = "related_entity_id", length = 60)
    private String relatedEntityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isRead() { return readAt != null; }

    public void markRead() {
        if (readAt == null) readAt = Instant.now();
    }
}
