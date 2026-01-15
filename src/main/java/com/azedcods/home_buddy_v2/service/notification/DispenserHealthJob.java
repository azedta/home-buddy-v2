package com.azedcods.home_buddy_v2.service.notification;

import com.azedcods.home_buddy_v2.enums.*;
import com.azedcods.home_buddy_v2.model.dispenser.Dispenser;
import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.repository.dispenser.DispenserCompartmentRepository;
import com.azedcods.home_buddy_v2.repository.dispenser.DispenserRepository;
import com.azedcods.home_buddy_v2.service.notification.NotificationEngine;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class DispenserHealthJob {

    // tune later
    private static final int LOW_TOTAL_PILLS_THRESHOLD = 20;

    private final DispenserRepository dispenserRepo;
    private final DispenserCompartmentRepository compartmentRepo;
    private final NotificationEngine notificationEngine;

    public DispenserHealthJob(
            DispenserRepository dispenserRepo,
            DispenserCompartmentRepository compartmentRepo,
            NotificationEngine notificationEngine
    ) {
        this.dispenserRepo = dispenserRepo;
        this.compartmentRepo = compartmentRepo;
        this.notificationEngine = notificationEngine;
    }

    @Scheduled(fixedRate = 5 * 60_000) // every 5 minutes
    @Transactional
    public void tickDispenserHealth() {
        List<Dispenser> dispensers = dispenserRepo.findAll();
        if (dispensers.isEmpty()) return;

        for (Dispenser d : dispensers) {
            Robot r = d.getRobot();
            if (r == null || r.getAssistedUser() == null) continue;

            String robotId = r.getId();
            Long userId = r.getAssistedUser().getUserId();

            long total = compartmentRepo.sumPillsForRobot(robotId);

            if (total <= 0) {
                String key = "DISPENSER_EMPTY:robot=" + robotId + ":user=" + userId;

                notificationEngine.emit(
                        NotificationRule.DISPENSER_EMPTY,
                        userId,
                        key,
                        NotificationType.DISPENSER,
                        NotificationSeverity.CRITICAL,
                        "Dispenser empty",
                        "No pills are available in the dispenser. Refill required.",
                        "/medication?tab=dispenser",
                        Duration.ofHours(6)
                );

            } else if (total <= LOW_TOTAL_PILLS_THRESHOLD) {
                String key = "DISPENSER_LOW:robot=" + robotId + ":user=" + userId;

                notificationEngine.emit(
                        NotificationRule.DISPENSER_LOW,
                        userId,
                        key,
                        NotificationType.DISPENSER,
                        NotificationSeverity.WARN,
                        "Dispenser running low",
                        "Only " + total + " pill(s) remaining. Plan a refill soon.",
                        "/medication?tab=dispenser",
                        Duration.ofHours(6)
                );
            }
        }
    }
}
