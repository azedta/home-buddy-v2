package com.azedcods.home_buddy_v2.service;

import com.azedcods.home_buddy_v2.model.Robot;
import com.azedcods.home_buddy_v2.model.RobotCommand;
import com.azedcods.home_buddy_v2.model.enums.*;
import com.azedcods.home_buddy_v2.repository.RobotCommandRepository;
import com.azedcods.home_buddy_v2.repository.RobotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RobotCommandService {

    private final RobotBootstrapService bootstrap;
    private final RobotRepository robotRepo;
    private final RobotCommandRepository commandRepo;
    private final RobotActivityService activityService;

    public RobotCommandService(
            RobotBootstrapService bootstrap,
            RobotRepository robotRepo,
            RobotCommandRepository commandRepo,
            RobotActivityService activityService
    ) {
        this.bootstrap = bootstrap;
        this.robotRepo = robotRepo;
        this.commandRepo = commandRepo;
        this.activityService = activityService;
    }

    @Transactional
    public RobotCommand issue(CommandType type, String targetLocation, String description) {
        Robot r = bootstrap.getOrCreateRobot();

        RobotCommand cmd = new RobotCommand();
        cmd.setRobot(r);
        cmd.setCommandTime(Instant.now());
        cmd.setCommandType(type);
        cmd.setTargetLocation(targetLocation);
        cmd.setDescription(description != null ? description : type.name());
        cmd.setStatus(CommandStatus.EXECUTED);

        // Apply minimal “effect” to simulation so it looks real
        switch (type) {
            case MOVE_TO_LOCATION -> {
                String target = (targetLocation == null || targetLocation.isBlank()) ? "LIVING_ROOM" : targetLocation;
                r.setTargetLocation(target);
                r.setRobotStatus(RobotStatus.MOVING_TO_LOCATION);
                r.setTrayStatus(TrayStatus.DOWN);
                activityService.log(r, ActivityType.COMMAND, ActivitySeverity.INFO,
                        "Command received: MOVE_TO_LOCATION → " + target + ". " + cmd.getDescription());
            }
            case RETURN_TO_DOCK -> {
                r.setTargetLocation("DOCK");
                r.setRobotStatus(RobotStatus.RETURNING_TO_DOCK);
                r.setTrayStatus(TrayStatus.DOWN);
                activityService.log(r, ActivityType.COMMAND, ActivitySeverity.WARN,
                        "Command received: RETURN_TO_DOCK. " + cmd.getDescription());
            }
            case DELIVER_ITEMS -> {
                // deliver means move then stop and raise tray
                String target = (targetLocation == null || targetLocation.isBlank()) ? "LIVING_ROOM" : targetLocation;
                r.setTargetLocation(target);
                r.setRobotStatus(RobotStatus.DELIVERING);
                r.setTrayStatus(TrayStatus.DOWN);
                activityService.log(r, ActivityType.COMMAND, ActivitySeverity.INFO,
                        "Command received: DELIVER_ITEMS → " + target + ". " + cmd.getDescription());
            }
        }

        r.setLastUpdatedAt(Instant.now());
        robotRepo.save(r);

        return commandRepo.save(cmd);
    }
}