package com.azedcods.home_buddy_v2.service.robot;

import com.azedcods.home_buddy_v2.enums.*;
import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.model.robot.RobotActivity;
import com.azedcods.home_buddy_v2.repository.robot.RobotActivityRepository;
import com.azedcods.home_buddy_v2.service.notification.NotificationEngine;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class RobotActivityService {

    private static final int MAX_ACTIVITIES_PER_ROBOT = 100;

    // STEP 6 tuning (anti-spam + realism)
    private static final Duration MOVEMENT_ESCALATION_WINDOW = Duration.ofMinutes(5);
    private static final int MOVEMENT_WARN_THRESHOLD = 2; // notify only if >= 2 WARNs in 5 min

    private final RobotActivityRepository activityRepo;
    private final NotificationEngine notificationEngine;

    public RobotActivityService(
            RobotActivityRepository activityRepo,
            NotificationEngine notificationEngine
    ) {
        this.activityRepo = activityRepo;
        this.notificationEngine = notificationEngine;
    }

    @Transactional
    public RobotActivity log(Robot robot, ActivityType type, ActivitySeverity severity, String desc) {
        RobotActivity a = new RobotActivity();
        a.setRobot(robot);
        a.setActivityTime(Instant.now());
        a.setActivityType(type);
        a.setSeverity(severity);
        a.setActivityDescription(desc);

        RobotActivity saved = activityRepo.save(a);
        trimToMax(robot.getId());

        // ✅ Additive: does not affect activity log behavior
        emitNotificationIfNeeded(robot, type, severity, desc);

        return saved;
    }

    @Transactional
    public void trimToMax(String robotId) {
        long count = activityRepo.countByRobot_Id(robotId);
        if (count <= MAX_ACTIVITIES_PER_ROBOT) return;

        int overflow = (int) (count - MAX_ACTIVITIES_PER_ROBOT);
        List<RobotActivity> oldest = activityRepo.findByRobot_IdOrderByActivityTimeAsc(
                robotId, PageRequest.of(0, overflow)
        );
        activityRepo.deleteAll(oldest);
    }

    /* ----------------------- STEP 6: smarter notifications ----------------------- */

    private void emitNotificationIfNeeded(Robot robot, ActivityType type, ActivitySeverity severity, String desc) {
        if (robot == null) return;
        if (robot.getAssistedUser() == null || robot.getAssistedUser().getUserId() == null) return;

        if (severity == null) return;

        // Only notify for WARN/ERROR (INFO stays in activity feed)
        boolean shouldNotify = (severity == ActivitySeverity.WARN || severity == ActivitySeverity.ERROR);
        if (!shouldNotify) return;

        // STEP 6 anti-spam: movement WARN only notifies if repeated in the last N minutes
        if (type == ActivityType.MOVEMENT && severity == ActivitySeverity.WARN) {
            if (!shouldEscalateMovement(robot.getId())) return;
        }

        Long userId = robot.getAssistedUser().getUserId();
        String robotId = robot.getId();

        NotificationRule rule = mapRule(type, severity, desc);

        NotificationSeverity nSev = mapNotificationSeverity(severity, rule);

        NotificationType nType = mapNotificationType(rule);

        String title = titleFor(rule, robot);
        String message = (desc == null || desc.isBlank())
                ? ("Robot event: " + (type == null ? "UNKNOWN" : type.name()))
                : desc.trim();

        String actionUrl = actionUrlFor(rule);

        // Stable dedupe key (rule + robot + optional signature)
        String key = buildKey(rule, robotId, message, type);

        Duration cooldown = cooldownFor(rule);

        notificationEngine.emit(
                rule,
                userId,
                key,
                nType,
                nSev,
                title,
                message,
                actionUrl,
                cooldown
        );
    }

    private boolean shouldEscalateMovement(String robotId) {
        Instant since = Instant.now().minus(MOVEMENT_ESCALATION_WINDOW);

        long count = activityRepo.countRecentByTypeAndSeverities(
                robotId,
                ActivityType.MOVEMENT,
                List.of(ActivitySeverity.WARN, ActivitySeverity.ERROR),
                since
        );

        return count >= MOVEMENT_WARN_THRESHOLD;
    }

    private NotificationSeverity mapNotificationSeverity(ActivitySeverity severity, NotificationRule rule) {
        // Your ActivitySeverity does NOT have CRITICAL; it has ERROR.
        // We map ERROR -> CRITICAL for notifications.
        if (severity == ActivitySeverity.ERROR) return NotificationSeverity.CRITICAL;
        if (severity == ActivitySeverity.WARN) return NotificationSeverity.WARN;

        // Some rules are "good news" but come via warnings occasionally
        if (rule == NotificationRule.ROBOT_RECOVERED) return NotificationSeverity.SUCCESS;

        return NotificationSeverity.INFO;
    }

    private NotificationType mapNotificationType(NotificationRule rule) {
        // Keep simple & consistent with your UI filters
        return switch (rule) {
            case DISPENSER_EMPTY, DISPENSER_LOW, DISPENSE_FAILED, DISPENSE_SUCCESS -> NotificationType.DISPENSER;
            case DOSE_DUE, DOSE_MISSED, DOSE_TAKEN, DOSE_CONFIRM_REQUIRED -> NotificationType.MEDICATION;
            case LOGIN_NEW_DEVICE, LOGIN_FAILED_ATTEMPTS, PERMISSION_DENIED -> NotificationType.SECURITY;
            default -> NotificationType.ROBOT;
        };
    }

    private NotificationRule mapRule(ActivityType type, ActivitySeverity severity, String desc) {
        if (type == null) return NotificationRule.SYSTEM_INTEGRITY_WARNING;

        String d = (desc == null) ? "" : desc.toLowerCase();

        return switch (type) {
            case BATTERY -> {
                if (severity == ActivitySeverity.ERROR || d.contains("critical") || d.contains("10%")) {
                    yield NotificationRule.ROBOT_BATTERY_CRITICAL;
                }
                yield NotificationRule.ROBOT_BATTERY_LOW;
            }

            case DISPENSING -> {
                // If message suggests empty/refill -> dispenser empty
                if (d.contains("empty") || d.contains("refill")) yield NotificationRule.DISPENSER_EMPTY;
                // If ERROR -> dispense failed, else treat as issue detected (still fail category)
                yield NotificationRule.DISPENSE_FAILED;
            }

            case MOVEMENT -> {
                // WARN/ERROR movement becomes "stuck"
                yield NotificationRule.ROBOT_STUCK;
            }

            case SENSORS -> {
                // If sensors disabled, we notify (warn)
                yield NotificationRule.ROBOT_SENSOR_DISABLED;
            }

            default -> NotificationRule.SYSTEM_INTEGRITY_WARNING;
        };
    }

    private String titleFor(NotificationRule rule, Robot robot) {
        String name = (robot != null && robot.getRobotName() != null && !robot.getRobotName().isBlank())
                ? robot.getRobotName().trim()
                : "Robot";

        return switch (rule) {
            case ROBOT_BATTERY_CRITICAL -> name + " battery critical";
            case ROBOT_BATTERY_LOW -> name + " battery low";
            case ROBOT_DOWN -> name + " is offline";
            case ROBOT_RECOVERED -> name + " restored";
            case ROBOT_STUCK -> name + " navigation issue";
            case ROBOT_SENSOR_DISABLED -> name + " sensor warning";
            case DISPENSER_EMPTY -> "Dispenser empty";
            case DISPENSER_LOW -> "Dispenser running low";
            case DISPENSE_FAILED -> "Dispense issue detected";
            default -> "Robot alert";
        };
    }

    private String actionUrlFor(NotificationRule rule) {
        // STEP 6: make actions feel real + useful
        return switch (rule) {
            case ROBOT_DOWN -> "/robot/telemetry";
            case ROBOT_STUCK -> "/robot/location";
            case ROBOT_SENSOR_DISABLED -> "/robot";
            case ROBOT_BATTERY_LOW, ROBOT_BATTERY_CRITICAL -> "/robot";
            case DISPENSER_EMPTY, DISPENSER_LOW, DISPENSE_FAILED -> "/medication?tab=dispenser";
            default -> "/robot";
        };
    }

    private Duration cooldownFor(NotificationRule rule) {
        // Conservative defaults: avoid notification spam
        return switch (rule) {
            case ROBOT_BATTERY_CRITICAL -> Duration.ofMinutes(15);
            case ROBOT_BATTERY_LOW -> Duration.ofMinutes(30);
            case ROBOT_STUCK -> Duration.ofMinutes(20);
            case ROBOT_SENSOR_DISABLED -> Duration.ofMinutes(60);

            case DISPENSER_EMPTY -> Duration.ofHours(6);
            case DISPENSER_LOW -> Duration.ofHours(6);
            case DISPENSE_FAILED -> Duration.ofMinutes(30);

            default -> Duration.ofMinutes(30);
        };
    }

    private String buildKey(NotificationRule rule, String robotId, String message, ActivityType type) {
        // Stable key per rule + robot, plus a light signature for variety (prevents clobbering unrelated events)
        String sig = signature(message);

        String t = (type == null) ? "UNKNOWN" : type.name();
        return rule.name() + ":robot=" + robotId + ":type=" + t + (sig == null ? "" : (":sig=" + sig));
    }

    private String signature(String desc) {
        if (desc == null) return null;
        String d = desc.trim();
        if (d.isEmpty()) return null;

        // lightweight stable signature
        int h = Objects.hash(d.length(), d.charAt(0), d.charAt(d.length() - 1), d);
        return Integer.toHexString(h);
    }
}
