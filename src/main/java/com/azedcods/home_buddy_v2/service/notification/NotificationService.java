// src/main/java/com/azedcods/home_buddy_v2/service/notification/NotificationService.java
package com.azedcods.home_buddy_v2.service.notification;

import com.azedcods.home_buddy_v2.enums.NotificationSeverity;
import com.azedcods.home_buddy_v2.enums.NotificationType;
import com.azedcods.home_buddy_v2.model.notif.Notification;
import com.azedcods.home_buddy_v2.model.auth.User;
import com.azedcods.home_buddy_v2.payload.NotificationDtos;
import com.azedcods.home_buddy_v2.repository.notif.NotificationRepository;
import com.azedcods.home_buddy_v2.repository.auth.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static org.springframework.data.jpa.domain.Specification.where;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_NOTIFICATIONS_PER_USER = 500;

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    public Page<NotificationDtos.Response> list(
            Long userId,
            Boolean unreadOnly,
            NotificationType type,
            Instant from,
            Instant to,
            String q,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        var spec = where(NotificationSpecs.recipient(userId))
                .and(NotificationSpecs.unreadOnly(unreadOnly))
                .and(NotificationSpecs.type(type))
                .and(NotificationSpecs.from(from))
                .and(NotificationSpecs.to(to))
                .and(NotificationSpecs.search(q));

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepo.findAll(spec, pageable).map(this::toDto);
    }

    // ✅ supports admin-all mode
    public NotificationDtos.UnreadCountResponse unreadCount(Long userId) {
        long count = (userId == null) ? notificationRepo.countUnreadAll() : notificationRepo.countUnread(userId);
        return new NotificationDtos.UnreadCountResponse(userId, count);
    }

    @Transactional
    public NotificationDtos.Response create(NotificationDtos.CreateRequest req) {
        User recipient = userRepo.findById(req.recipientUserId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found: " + req.recipientUserId()));

        User actor = null;
        if (req.actorUserId() != null) {
            actor = userRepo.findById(req.actorUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Actor user not found: " + req.actorUserId()));
        }

        NotificationType type = req.type() != null ? req.type() : NotificationType.GENERAL;
        NotificationSeverity sev = req.severity() != null ? req.severity() : NotificationSeverity.INFO;

        Notification n = new Notification();
        n.setRecipientUser(recipient);
        n.setActorUser(actor);

        n.setType(type);
        n.setSeverity(sev);

        n.setTitle(safe(req.title(), 180, "Notification"));
        n.setMessage(safe(req.message(), 2000, ""));

        n.setSourceModule(safe(req.sourceModule(), 60, null));
        n.setActionUrl(safe(req.actionUrl(), 300, null));

        n.setRelatedEntityType(safe(req.relatedEntityType(), 60, null));
        n.setRelatedEntityId(safe(req.relatedEntityId(), 60, null));

        notificationRepo.save(n);

        pruneOldestForUser(recipient.getUserId());
        return toDto(n);
    }

    @Transactional
    public NotificationDtos.Response markRead(Long notificationId, Long userId) {
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!n.getRecipientUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to userId=" + userId);
        }

        n.markRead();
        return toDto(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepo.markAllRead(userId, Instant.now());
    }

    @Transactional
    public void delete(Long notificationId) {
        if (!notificationRepo.existsById(notificationId)) return;
        notificationRepo.deleteById(notificationId);
    }

    private void pruneOldestForUser(Long userId) {
        long count = notificationRepo.countByRecipientUser_UserId(userId);
        if (count <= MAX_NOTIFICATIONS_PER_USER) return;

        int overflow = (int) (count - MAX_NOTIFICATIONS_PER_USER);
        List<Notification> oldest = notificationRepo.findByRecipientUser_UserIdOrderByCreatedAtAsc(
                userId,
                PageRequest.of(0, overflow)
        );
        notificationRepo.deleteAll(oldest);
    }

    private NotificationDtos.Response toDto(Notification n) {
        return new NotificationDtos.Response(
                n.getId(),
                n.getRecipientUser().getUserId(),
                n.getActorUser() != null ? n.getActorUser().getUserId() : null,
                n.getRule(),
                n.getNotificationKey(),
                n.getType(),
                n.getSeverity(),
                n.getTitle(),
                n.getMessage(),
                n.getSourceModule(),
                n.getActionUrl(),
                n.getRelatedEntityType(),
                n.getRelatedEntityId(),
                n.getCreatedAt(),
                n.getReadAt(),
                n.isRead()
        );
    }

    private String safe(String s, int max, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        if (t.isEmpty()) return fallback;
        return t.length() <= max ? t : t.substring(0, max);
    }
}
