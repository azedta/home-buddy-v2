package com.azedcods.home_buddy_v2.service.notification;

import com.azedcods.home_buddy_v2.enums.*;
import com.azedcods.home_buddy_v2.model.notif.Notification;
import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.repository.notif.NotificationRepository;
import com.azedcods.home_buddy_v2.repository.robot.RobotRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RobotHealthJob {

    private final RobotRepository robotRepo;
    private final NotificationEngine notificationEngine;
    private final NotificationRepository notificationRepo;

    // Tune later
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(20);

    public RobotHealthJob(
            RobotRepository robotRepo,
            NotificationEngine notificationEngine,
            NotificationRepository notificationRepo
    ) {
        this.robotRepo = robotRepo;
        this.notificationEngine = notificationEngine;
        this.notificationRepo = notificationRepo;
    }

    @Scheduled(fixedRate = 10_000) // every 10s
    @Transactional
    public void tickRobotHealth() {
        List<Robot> robots = robotRepo.findAll();
        if (robots.isEmpty()) return;

        Instant now = Instant.now();

        for (Robot r : robots) {
            if (r.getAssistedUser() == null || r.getAssistedUser().getUserId() == null) continue;

            Long userId = r.getAssistedUser().getUserId();
            String robotId = r.getId();

            Instant last = r.getLastUpdatedAt();
            boolean down = (last == null) || last.isBefore(now.minus(HEARTBEAT_TIMEOUT));

            String downKey = "ROBOT_DOWN:robot=" + robotId + ":user=" + userId;
            String recKey  = "ROBOT_RECOVERED:robot=" + robotId + ":user=" + userId;

            if (down) {
                notificationEngine.emit(
                        NotificationRule.ROBOT_DOWN,
                        userId,
                        downKey,
                        NotificationType.ROBOT,
                        NotificationSeverity.CRITICAL,
                        "Robot offline",
                        "Robot has not reported updates recently. Last seen: " + (last == null ? "never" : last.toString()),
                        "/robot",
                        Duration.ofMinutes(15)
                );
            } else {
                // Emit RECOVERED only if a DOWN notification exists recently (avoids false “recovered” spam)
                Optional<Notification> recentDown =
                        notificationRepo.findTopByRecipientUser_UserIdAndNotificationKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                                userId,
                                downKey,
                                now.minus(Duration.ofHours(6))
                        );


                if (recentDown.isPresent()) {
                    notificationEngine.emit(
                            NotificationRule.ROBOT_RECOVERED,
                            userId,
                            recKey,
                            NotificationType.ROBOT,
                            NotificationSeverity.SUCCESS,
                            "Robot restored",
                            "Robot connection is back and telemetry updates resumed.",
                            "/robot",
                            Duration.ofMinutes(30)
                    );
                }
            }
        }
    }
}
