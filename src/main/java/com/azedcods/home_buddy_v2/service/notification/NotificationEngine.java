package com.azedcods.home_buddy_v2.service.notification;

import com.azedcods.home_buddy_v2.enums.*;
import com.azedcods.home_buddy_v2.model.auth.User;
import com.azedcods.home_buddy_v2.model.notif.Notification;
import com.azedcods.home_buddy_v2.repository.notif.NotificationRepository;
import com.azedcods.home_buddy_v2.repository.auth.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationEngine {

    private static final int MAX_NOTIFICATIONS_PER_USER = 500;

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    /**
     * Emit a notification with dedupe/cooldown.
     * If another notification with the same (userId + key) was created within cooldown, it will be skipped.
     *
     * @return created Notification, or empty if skipped due to cooldown.
     */
    @Transactional
    public Optional<Notification> emit(EmitRequest req) {
        if (req == null) return Optional.empty();
        if (req.recipientUserId() == null) return Optional.empty();
        if (req.rule() == null) return Optional.empty();
        if (req.type() == null) return Optional.empty();
        if (req.severity() == null) return Optional.empty();

        User recipient = userRepo.findById(req.recipientUserId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found: " + req.recipientUserId()));

        User actor = null;
        if (req.actorUserId() != null) {
            actor = userRepo.findById(req.actorUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Actor user not found: " + req.actorUserId()));
        }

        String key = safe(req.notificationKey(), 200, null);

        // DEDUPE: if key exists, enforce cooldown
        if (key != null && !key.isBlank()) {
            Duration cd = req.cooldown() != null ? req.cooldown() : Duration.ofMinutes(10);
            Instant since = Instant.now().minus(cd);

            Optional<Notification> recent =
                    notificationRepo.findTopByRecipientUser_UserIdAndNotificationKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                            recipient.getUserId(),
                            key,
                            since
                    );

            if (recent.isPresent()) {
                return Optional.empty(); // skipped
            }
        }

        Notification n = new Notification();
        n.setRecipientUser(recipient);
        n.setActorUser(actor);

        n.setRule(req.rule());
        n.setNotificationKey(key);

        n.setType(req.type());
        n.setSeverity(req.severity());

        n.setTitle(safe(req.title(), 180, "Notification"));
        n.setMessage(safe(req.message(), 2000, ""));

        n.setSourceModule(safe(req.sourceModule(), 60, null));
        n.setActionUrl(safe(req.actionUrl(), 300, null));

        n.setRelatedEntityType(safe(req.relatedEntityType(), 60, null));
        n.setRelatedEntityId(safe(req.relatedEntityId(), 60, null));

        notificationRepo.save(n);
        pruneOldestForUser(recipient.getUserId());
        return Optional.of(n);
    }


    private void pruneOldestForUser(Long userId) {
        long count = notificationRepo.countByRecipientUser_UserId(userId);
        if (count <= MAX_NOTIFICATIONS_PER_USER) return;

        int overflow = (int) (count - MAX_NOTIFICATIONS_PER_USER);
        var oldest = notificationRepo.findByRecipientUser_UserIdOrderByCreatedAtAsc(
                userId,
                org.springframework.data.domain.PageRequest.of(0, overflow)
        );
        notificationRepo.deleteAll(oldest);
    }


    // Convenience overload (common use)
    @Transactional
    public Optional<Notification> emit(
            NotificationRule rule,
            Long recipientUserId,
            String notificationKey,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String actionUrl,
            Duration cooldown
    ) {
        return emit(new EmitRequest(
                rule,
                recipientUserId,
                null,
                notificationKey,
                type,
                severity,
                title,
                message,
                "NotificationEngine",
                actionUrl,
                null,
                null,
                cooldown
        ));
    }

    public record EmitRequest(
            NotificationRule rule,
            Long recipientUserId,
            Long actorUserId,
            String notificationKey,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String sourceModule,
            String actionUrl,
            String relatedEntityType,
            String relatedEntityId,
            Duration cooldown
    ) {}

    private String safe(String s, int max, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        if (t.isEmpty()) return fallback;
        return t.length() <= max ? t : t.substring(0, max);
    }
}
