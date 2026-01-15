package com.azedcods.home_buddy_v2.service.robot;

import com.azedcods.home_buddy_v2.enums.FillLevel;
import com.azedcods.home_buddy_v2.enums.RobotStatus;
import com.azedcods.home_buddy_v2.enums.ToggleState;
import com.azedcods.home_buddy_v2.enums.TrayStatus;
import com.azedcods.home_buddy_v2.model.auth.User;
import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.repository.robot.RobotRepository;
import com.azedcods.home_buddy_v2.service.dispenser.DispenserBootstrapService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

import static com.azedcods.home_buddy_v2.enums.HouseLocation.DOCK;

@Service
public class RobotBootstrapService {

    private final RobotRepository robotRepo;
    private final DispenserBootstrapService dispenserBootstrapService;
    private final SecureRandom rng = new SecureRandom();

    public RobotBootstrapService(
            RobotRepository robotRepo,
            DispenserBootstrapService dispenserBootstrapService
    ) {
        this.robotRepo = robotRepo;
        this.dispenserBootstrapService = dispenserBootstrapService;
    }

    /**
     * Creates (or returns) the robot assigned to this user.
     * Enforces: 1 user -> 1 robot (unique assisted_user_id).
     *
     * ALSO ensures: 1 robot -> 1 dispenser (auto-created).
     */
    @Transactional
    public Robot getOrCreateRobotForUser(User user) {
        Robot robot = robotRepo.findByAssistedUserUserId(user.getUserId()).orElseGet(() -> {
            Robot r = new Robot();

            // COOL ID like: LEO-4523124-VAC
            r.setId(generateCoolRobotId("LEO"));

            r.setAssistedUser(user);

            // Defaults (same as your prototype)
            r.setBatteryLevel(92);
            r.setRobotStatus(RobotStatus.RESTING);
            r.setTrayStatus(TrayStatus.UP);

            r.setSensorStatus(ToggleState.ON);
            r.setSensorMessage("All sensors operational");

            r.setDispenserStatus(ToggleState.ON);
            r.setDispenserPillsRemaining(18);
            r.setDispenserFillLevel(FillLevel.FULL);

            r.setCurrentLocation(DOCK);
            r.setTargetLocation(DOCK);
            r.setLastUpdatedAt(Instant.now());

            return robotRepo.save(r);
        });

        // Always ensure dispenser exists (idempotent)
        dispenserBootstrapService.ensureForRobot(robot.getId());

        return robot;
    }

    /**
     * Admin/caregiver can create unassigned robots too (optional use-case).
     * ALSO ensures dispenser exists for that robot.
     */
    @Transactional
    public Robot createUnassignedRobot(String modelCode) {
        Robot r = new Robot();
        r.setId(generateCoolRobotId(modelCode == null || modelCode.isBlank()
                ? "LEO"
                : modelCode.trim().toUpperCase()));

        r.setBatteryLevel(100);
        r.setRobotStatus(RobotStatus.RESTING);
        r.setTrayStatus(TrayStatus.UP);

        r.setSensorStatus(ToggleState.ON);
        r.setSensorMessage("All sensors operational");

        r.setDispenserStatus(ToggleState.ON);
        r.setDispenserPillsRemaining(0);
        r.setDispenserFillLevel(FillLevel.EMPTY);

        r.setCurrentLocation(DOCK);
        r.setTargetLocation(DOCK);
        r.setLastUpdatedAt(Instant.now());

        Robot saved = robotRepo.save(r);

        // Ensure dispenser exists (idempotent)
        dispenserBootstrapService.ensureForRobot(saved.getId());

        return saved;
    }

    private String generateCoolRobotId(String model) {
        final String[] suffixPool = {"VAC", "MED", "AID", "NAV", "SEN", "CAR", "BOT"};

        String id;
        int tries = 0;
        do {
            int digits = 1_000_000 + rng.nextInt(9_000_000); // 7 digits
            String suffix = suffixPool[rng.nextInt(suffixPool.length)];
            id = model + "-" + digits + "-" + suffix;
            tries++;
            if (tries > 50) {
                id = model + "-" + System.currentTimeMillis() + "-" + suffix;
                break;
            }
        } while (robotRepo.existsById(id));

        return id;
    }
}
