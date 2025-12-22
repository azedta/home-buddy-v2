package com.azedcods.home_buddy_v2.service;

import com.azedcods.home_buddy_v2.model.Robot;
import com.azedcods.home_buddy_v2.model.enums.*;
import com.azedcods.home_buddy_v2.model.enums.RobotStatus;
import com.azedcods.home_buddy_v2.repository.RobotRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
public class RobotSimulationService {

    private final RobotBootstrapService bootstrap;
    private final RobotRepository robotRepo;
    private final RobotActivityService activityService;

    private final Random random = new Random();

    private static final int LOW_BATTERY_THRESHOLD = 20;
    private static final int FULL_BATTERY = 100;

    private static final List<String> HOUSE_LOCATIONS = List.of(
            "LIVING_ROOM", "KITCHEN", "BEDROOM", "BATHROOM", "HALLWAY", "DINING_ROOM"
    );

    public RobotSimulationService(
            RobotBootstrapService bootstrap,
            RobotRepository robotRepo,
            RobotActivityService activityService
    ) {
        this.bootstrap = bootstrap;
        this.robotRepo = robotRepo;
        this.activityService = activityService;
    }

    // every 3 seconds: update LIVE robot state
    @Scheduled(fixedRate = 3000)
    @Transactional
    public void tickStatus() {
        Robot r = bootstrap.getOrCreateRobot();

        // 1) battery logic
        if (r.getRobotStatus() == RobotStatus.CHARGING) {
            int inc = 2 + random.nextInt(3); // +2..+4
            r.setBatteryLevel(Math.min(FULL_BATTERY, r.getBatteryLevel() + inc));
            r.setSensorMessage("Docked. Charging in progress.");

            if (r.getBatteryLevel() >= 95) {
                r.setRobotStatus(RobotStatus.RESTING);
                r.setTrayStatus(TrayStatus.UP);
                activityService.log(r, ActivityType.BATTERY, ActivitySeverity.INFO,
                        "Battery sufficiently charged (" + r.getBatteryLevel() + "%). Switching to RESTING.");
            }
        } else {
            int dec = (r.getRobotStatus() == RobotStatus.RESTING) ? 1 : (2 + random.nextInt(2)); // -1 or -2..-3
            r.setBatteryLevel(Math.max(0, r.getBatteryLevel() - dec));

            if (r.getBatteryLevel() <= LOW_BATTERY_THRESHOLD && r.getRobotStatus() != RobotStatus.RETURNING_TO_DOCK) {
                r.setRobotStatus(RobotStatus.RETURNING_TO_DOCK);
                r.setTargetLocation("DOCK");
                r.setTrayStatus(TrayStatus.DOWN);
                activityService.log(r, ActivityType.BATTERY, ActivitySeverity.WARN,
                        "Low battery (" + r.getBatteryLevel() + "%). Returning to dock.");
            }
        }

        // 2) movement + tray + sensor message
        switch (r.getRobotStatus()) {

            case RETURNING_TO_DOCK -> {
                r.setTrayStatus(TrayStatus.DOWN);
                r.setSensorMessage(randomSensorWhileMoving());
                // simulate "arrival" sometimes
                if (random.nextDouble() < 0.35) {
                    r.setCurrentLocation("DOCK");
                    r.setRobotStatus(RobotStatus.CHARGING);
                    activityService.log(r, ActivityType.MOVEMENT, ActivitySeverity.INFO,
                            "Arrived at charging dock.");
                } else {
                    r.setCurrentLocation("HALLWAY");
                }
            }

            case MOVING_TO_LOCATION, DELIVERING -> {
                r.setTrayStatus(TrayStatus.DOWN);
                r.setSensorMessage(randomSensorWhileMoving());

                if (random.nextDouble() < 0.40) {
                    // arrived
                    r.setCurrentLocation(r.getTargetLocation());
                    r.setTrayStatus(TrayStatus.UP);
                    r.setRobotStatus(RobotStatus.RESTING);

                    activityService.log(r, ActivityType.MOVEMENT, ActivitySeverity.INFO,
                            "Reached " + r.getCurrentLocation() + ". Stopping near user.");
                    activityService.log(r, ActivityType.TRAY, ActivitySeverity.INFO,
                            "Tray raised for safe handoff.");
                }
            }

            case RESTING -> {
                r.setTrayStatus(TrayStatus.UP);
                // occasionally decide to "do something"
                if (random.nextDouble() < 0.20 && r.getBatteryLevel() > LOW_BATTERY_THRESHOLD + 5) {
                    String next = randomHouseLocation();
                    r.setTargetLocation(next);
                    r.setRobotStatus(RobotStatus.MOVING_TO_LOCATION);
                    r.setTrayStatus(TrayStatus.DOWN);
                    activityService.log(r, ActivityType.MOVEMENT, ActivitySeverity.INFO,
                            "Starting navigation to " + next + ".");
                } else {
                    r.setSensorMessage("Idle. Monitoring environment.");
                }
            }

            case CHARGING -> {
                // already handled above
            }
        }

        // 3) dispenser virtual updates
        if (r.getDispenserStatus() == ToggleState.ON && r.getDispenserPillsRemaining() > 0) {
            // tiny chance of a dispensing event while resting
            if (r.getRobotStatus() == RobotStatus.RESTING && random.nextDouble() < 0.10) {
                r.setDispenserPillsRemaining(r.getDispenserPillsRemaining() - 1);
                activityService.log(r, ActivityType.DISPENSING, ActivitySeverity.INFO,
                        "Dispensed medication dose. Pills remaining: " + r.getDispenserPillsRemaining());
            }
        }

        if (r.getDispenserPillsRemaining() <= 0) {
            r.setDispenserFillLevel(FillLevel.EMPTY);
            activityService.log(r, ActivityType.DISPENSING, ActivitySeverity.WARN,
                    "Dispenser is empty. Refill required.");
        } else {
            r.setDispenserFillLevel(FillLevel.FULL);
        }

        r.setLastUpdatedAt(Instant.now());
        robotRepo.save(r);
    }

    // every 10 seconds: generate an additional “activity feed” item that makes sense
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void tickActivityFeed() {
        Robot r = bootstrap.getOrCreateRobot();

        // Keep it coherent with current state
        switch (r.getRobotStatus()) {
            case CHARGING ->
                    activityService.log(r, ActivityType.BATTERY, ActivitySeverity.INFO,
                            "Charging... battery now " + r.getBatteryLevel() + "%.");

            case RETURNING_TO_DOCK ->
                    activityService.log(r, ActivityType.SENSORS, ActivitySeverity.INFO,
                            "Navigation check: " + randomSensorWhileMoving());

            case MOVING_TO_LOCATION ->
                    activityService.log(r, ActivityType.MOVEMENT, ActivitySeverity.INFO,
                            "En route to " + r.getTargetLocation() + ". Battery " + r.getBatteryLevel() + "%.");

            case RESTING ->
                    activityService.log(r, ActivityType.SYSTEM, ActivitySeverity.INFO,
                            "Standing by at " + r.getCurrentLocation() + ". Monitoring user safety.");
            case DELIVERING ->
                    activityService.log(r, ActivityType.TRAY, ActivitySeverity.INFO,
                            "Delivery mode active. Tray status: " + r.getTrayStatus());
        }
    }

    private String randomHouseLocation() {
        return HOUSE_LOCATIONS.get(random.nextInt(HOUSE_LOCATIONS.size()));
    }

    private String randomSensorWhileMoving() {
        String[] samples = {
                "Obstacle detected at 0.9m — rerouting",
                "Clear path — maintaining speed",
                "Doorway detected — slowing down",
                "Human presence detected — yielding",
                "Carpet edge detected — adjusting traction",
                "Object detected — stopping briefly"
        };
        return samples[random.nextInt(samples.length)];
    }
}
