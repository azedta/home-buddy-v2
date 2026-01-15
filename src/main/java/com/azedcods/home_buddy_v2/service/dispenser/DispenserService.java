package com.azedcods.home_buddy_v2.service.dispenser;

import com.azedcods.home_buddy_v2.enums.NotificationRule;
import com.azedcods.home_buddy_v2.enums.NotificationSeverity;
import com.azedcods.home_buddy_v2.enums.NotificationType;
import com.azedcods.home_buddy_v2.model.dispenser.Dispenser;
import com.azedcods.home_buddy_v2.model.dispenser.DispenserCompartment;
import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.payload.DispenserDtos;
import com.azedcods.home_buddy_v2.repository.dispenser.DispenserCompartmentRepository;
import com.azedcods.home_buddy_v2.repository.dispenser.DispenserRepository;
import com.azedcods.home_buddy_v2.repository.robot.RobotRepository;
import com.azedcods.home_buddy_v2.service.notification.NotificationEngine;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class DispenserService {

    private static final int DEFAULT_COMPARTMENTS = 31;

    // tune later
    private static final int LOW_TOTAL_PILLS_THRESHOLD = 20;

    private final DispenserRepository dispenserRepo;
    private final DispenserCompartmentRepository compartmentRepo;
    private final RobotRepository robotRepo;

    private final NotificationEngine notificationEngine;

    public DispenserService(
            DispenserRepository dispenserRepo,
            DispenserCompartmentRepository compartmentRepo,
            RobotRepository robotRepo,
            NotificationEngine notificationEngine
    ) {
        this.dispenserRepo = dispenserRepo;
        this.compartmentRepo = compartmentRepo;
        this.robotRepo = robotRepo;
        this.notificationEngine = notificationEngine;
    }

    public Dispenser getOrCreateForRobot(String robotId) {
        return dispenserRepo.findByRobot_Id(robotId)
                .orElseGet(() -> createForRobot(robotId, DEFAULT_COMPARTMENTS));
    }

    private Dispenser createForRobot(String robotId, int compartmentsCount) {
        Robot robot = robotRepo.findById(robotId)
                .orElseThrow(() -> new EntityNotFoundException("Robot not found: " + robotId));

        dispenserRepo.findByRobot_Id(robotId).ifPresent(existing -> {
            throw new IllegalStateException("Dispenser already exists for robot: " + robotId);
        });

        Dispenser dispenser = new Dispenser(robot);
        dispenser.setLastRefilledAt(LocalDateTime.now());

        for (int day = 1; day <= compartmentsCount; day++) {
            dispenser.addCompartment(new DispenserCompartment(dispenser, day));
        }

        return dispenserRepo.save(dispenser);
    }

    @Transactional(readOnly = true)
    public List<DispenserCompartment> getCompartments(String robotId) {
        return compartmentRepo.findByDispenser_Robot_IdOrderByDayOfMonthAsc(robotId);
    }

    public Dispenser update(String robotId, DispenserDtos.UpdateRequest req) {
        Dispenser d = getOrCreateForRobot(robotId);

        if (req != null && req.hasSyrupHolder() != null) {
            d.setHasSyrupHolder(req.hasSyrupHolder());
        }

        return d;
    }

    public void resetMonth(String robotId) {
        getOrCreateForRobot(robotId);

        List<DispenserCompartment> compartments =
                compartmentRepo.findByDispenser_Robot_IdOrderByDayOfMonthAsc(robotId);

        for (DispenserCompartment c : compartments) {
            c.setPillsCount(0);
        }

        dispenserRepo.findByRobot_Id(robotId).ifPresent(d ->
                d.setLastRefilledAt(LocalDateTime.now())
        );

        emitSystemInfoToAssistedUser(robotId,
                "Dispenser reset",
                "The dispenser compartments were reset for a new cycle.",
                "DISPENSER_RESET:robot=" + robotId,
                Duration.ofMinutes(10)
        );
    }

    @Transactional(readOnly = true)
    public Dispenser getRequiredForRobot(String robotId) {
        return dispenserRepo.findByRobot_Id(robotId)
                .orElseThrow(() -> new EntityNotFoundException("Dispenser not found for robot: " + robotId));
    }

    /**
     * "Auto-load" compartments for a robot based on a day-of-month pill count map.
     * - We SET counts (idempotent)
     * - Values are clamped by each compartment's pillCapacity
     */
    public void applyDayLoad(String robotId, Map<Integer, Integer> pillsByDayOfMonth) {
        if (robotId == null || robotId.isBlank()) {
            throw new IllegalArgumentException("robotId is required");
        }

        getOrCreateForRobot(robotId);

        long beforeTotal = compartmentRepo.sumPillsForRobot(robotId);

        List<DispenserCompartment> compartments =
                compartmentRepo.findByDispenser_Robot_IdOrderByDayOfMonthAsc(robotId);

        for (DispenserCompartment c : compartments) {
            int day = c.getDayOfMonth() == null ? -1 : c.getDayOfMonth();
            int desired = (pillsByDayOfMonth == null) ? 0 : pillsByDayOfMonth.getOrDefault(day, 0);
            desired = Math.max(0, desired);

            Integer cap = c.getPillCapacity();
            if (cap != null && cap > 0) {
                desired = Math.min(desired, cap);
            }

            c.setPillsCount(desired);
        }

        dispenserRepo.findByRobot_Id(robotId).ifPresent(d -> d.setLastRefilledAt(LocalDateTime.now()));

        long afterTotal = compartmentRepo.sumPillsForRobot(robotId);

        if (beforeTotal <= 0 && afterTotal > 0) {
            emitToAssistedUser(robotId,
                    NotificationRule.DISPENSE_SUCCESS,
                    NotificationType.DISPENSER,
                    NotificationSeverity.SUCCESS,
                    "Dispenser loaded",
                    "The dispenser was loaded. Total pills available: " + afterTotal + ".",
                    "/medication?tab=dispenser",
                    "DISPENSER_LOADED:robot=" + robotId,
                    Duration.ofMinutes(30)
            );
        }

        emitStockState(robotId, afterTotal);
    }

    /**
     * ✅ NEW: When a dose is TAKEN, physically dispense 1 pill from the correct day compartment.
     * - Never goes below 0
     * - Emits low/empty notifications after dispensing
     */
    public void dispenseForScheduledTime(String robotId, LocalDateTime scheduledAt) {
        if (robotId == null || robotId.isBlank()) throw new IllegalArgumentException("robotId is required");
        if (scheduledAt == null) throw new IllegalArgumentException("scheduledAt is required");

        getOrCreateForRobot(robotId);

        int day = scheduledAt.getDayOfMonth();

        DispenserCompartment c = compartmentRepo
                .findByDispenser_Robot_IdAndDayOfMonth(robotId, day)
                .orElseThrow(() -> new IllegalStateException("Compartment not found for day " + day));

        int current = c.getPillsCount() == null ? 0 : c.getPillsCount();
        if (current <= 0) {
            // already empty for that day -> realistic warning
            emitToAssistedUser(robotId,
                    NotificationRule.DISPENSER_EMPTY,
                    NotificationType.DISPENSER,
                    NotificationSeverity.CRITICAL,
                    "Dispenser empty",
                    "No pills available for day " + day + ". Refill required.",
                    "/medication?tab=dispenser",
                    "DISPENSER_EMPTY:robot=" + robotId + ":day=" + day,
                    Duration.ofHours(6)
            );
            return;
        }

        c.setPillsCount(current - 1);

        long afterTotal = compartmentRepo.sumPillsForRobot(robotId);
        emitStockState(robotId, afterTotal);
    }

    /* ------------------------- robot resolution ------------------------- */

    @Transactional(readOnly = true)
    public String resolveRobotIdForUser(Long userId) {
        if (userId == null) return null;

        return robotRepo.findAll().stream()
                .filter(r -> r.getAssistedUser() != null && Objects.equals(r.getAssistedUser().getUserId(), userId))
                .map(Robot::getId)
                .findFirst()
                .orElse(null);
    }

    /* ------------------------- notifications ------------------------- */

    private void emitStockState(String robotId, long totalPills) {
        if (totalPills <= 0) {
            emitToAssistedUser(robotId,
                    NotificationRule.DISPENSER_EMPTY,
                    NotificationType.DISPENSER,
                    NotificationSeverity.CRITICAL,
                    "Dispenser empty",
                    "No pills are available in the dispenser. Refill required.",
                    "/medication?tab=dispenser",
                    "DISPENSER_EMPTY:robot=" + robotId,
                    Duration.ofHours(6)
            );
        } else if (totalPills <= LOW_TOTAL_PILLS_THRESHOLD) {
            emitToAssistedUser(robotId,
                    NotificationRule.DISPENSER_LOW,
                    NotificationType.DISPENSER,
                    NotificationSeverity.WARN,
                    "Dispenser running low",
                    "Only " + totalPills + " pill(s) remaining. Plan a refill soon.",
                    "/medication?tab=dispenser",
                    "DISPENSER_LOW:robot=" + robotId,
                    Duration.ofHours(6)
            );
        }
    }

    private void emitToAssistedUser(
            String robotId,
            NotificationRule rule,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String actionUrl,
            String key,
            Duration cooldown
    ) {
        Robot robot = robotRepo.findById(robotId).orElse(null);
        if (robot == null || robot.getAssistedUser() == null) return;

        Long userId = robot.getAssistedUser().getUserId();
        notificationEngine.emit(rule, userId, key, type, severity, title, message, actionUrl, cooldown);
    }

    private void emitSystemInfoToAssistedUser(String robotId, String title, String message, String key, Duration cooldown) {
        emitToAssistedUser(
                robotId,
                NotificationRule.SYSTEM_INTEGRITY_WARNING,
                NotificationType.SYSTEM,
                NotificationSeverity.INFO,
                title,
                message,
                "/medication?tab=dispenser",
                key,
                cooldown
        );
    }
}
