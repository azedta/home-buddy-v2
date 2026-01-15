package com.azedcods.home_buddy_v2.service.robot;

import com.azedcods.home_buddy_v2.enums.*;
import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.model.robot.RobotCommand;
import com.azedcods.home_buddy_v2.repository.robot.RobotCommandRepository;
import com.azedcods.home_buddy_v2.repository.robot.RobotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import static com.azedcods.home_buddy_v2.enums.CommandType.*;
import static com.azedcods.home_buddy_v2.util.EnumLabelUtil.getLabel;
import static com.azedcods.home_buddy_v2.enums.HouseLocation.*;

import java.time.Instant;

@Service
public class RobotCommandService {

    private final RobotRepository robotRepo;
    private final RobotCommandRepository commandRepo;
    private final RobotActivityService activityService;

    public RobotCommandService(
            RobotRepository robotRepo,
            RobotCommandRepository commandRepo,
            RobotActivityService activityService
    ) {
        this.robotRepo = robotRepo;
        this.commandRepo = commandRepo;
        this.activityService = activityService;
    }

    @Transactional
    public RobotCommand issue(String robotId, CommandType type, HouseLocation targetLocation, String description) {
        Robot r = robotRepo.findById(robotId)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found: " + robotId));

        RobotCommand cmd = new RobotCommand();
        cmd.setRobot(r);
        cmd.setCommandTime(Instant.now());
        cmd.setCommandType(type);
        cmd.setTargetLocation(targetLocation);
        cmd.setDescription(description == null ? "" : description.trim());
        cmd.setStatus(CommandStatus.QUEUED);

        // Apply minimal “effect” to simulation so it looks real
        switch(type) {
            case MOVE_TO_LOCATION -> {
                HouseLocation target = (targetLocation == null) ? LIVING_ROOM : targetLocation;
                r.setTargetLocation(target);
                r.setRobotStatus(RobotStatus.MOVING_TO_LOCATION);
                r.setTrayStatus(TrayStatus.DOWN);
                activityService.log(r, ActivityType.COMMAND, ActivitySeverity.INFO,
                        "Command received: " + getLabel(MOVE_TO_LOCATION) + " " + getLabel(target) + ". " + cmd.getDescription());

            }

            case DELIVER_ITEMS -> {
                // deliver means move then stop and raise tray
                HouseLocation target = (targetLocation == null ) ? LIVING_ROOM : targetLocation;
                r.setTargetLocation(target);
                r.setRobotStatus(RobotStatus.DELIVERING);
                r.setTrayStatus(TrayStatus.DOWN);
                activityService.log(r, ActivityType.COMMAND, ActivitySeverity.INFO,
                        "Command received: " +  getLabel(DELIVER_ITEMS) + " " + getLabel(target) + ". " + safe(description));

            }

            case RETURN_TO_DOCK -> {
                r.setTargetLocation(DOCK);
                r.setRobotStatus(RobotStatus.RETURNING_TO_DOCK);
                r.setTrayStatus(TrayStatus.DOWN);
                activityService.log(r, ActivityType.COMMAND, ActivitySeverity.WARN,
                        "Command received: " +  getLabel(RETURN_TO_DOCK) + cmd.getDescription());
            }
        }


        r.setLastUpdatedAt(Instant.now());
        robotRepo.save(r);

        return commandRepo.save(cmd);
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
