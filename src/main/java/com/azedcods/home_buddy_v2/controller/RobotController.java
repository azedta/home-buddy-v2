package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.enums.HouseLocation;
import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.payload.RobotDtos;
import com.azedcods.home_buddy_v2.repository.robot.RobotActivityRepository;
import com.azedcods.home_buddy_v2.repository.robot.RobotCommandRepository;
import com.azedcods.home_buddy_v2.repository.robot.RobotRepository;
import com.azedcods.home_buddy_v2.service.robot.RobotCommandService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/robot")
public class RobotController {

    private final RobotRepository robotRepo;
    private final RobotActivityRepository activityRepo;
    private final RobotCommandRepository commandRepo;
    private final RobotCommandService commandService;

    public RobotController(
            RobotRepository robotRepo,
            RobotActivityRepository activityRepo,
            RobotCommandRepository commandRepo,
            RobotCommandService commandService
    ) {
        this.robotRepo = robotRepo;
        this.activityRepo = activityRepo;
        this.commandRepo = commandRepo;
        this.commandService = commandService;
    }

    /**** ROBOTS ****/

    // List all robots (admin/caregiver view)
    @GetMapping
    public List<RobotDtos.StatusResponse> listRobots() {
        return robotRepo.findAll().stream().map(this::toStatus).toList();
    }

    // Status for ONE robot
    @GetMapping("/{robotId}/status")
    public RobotDtos.StatusResponse status(@PathVariable String robotId) {
        Robot r = robotRepo.findById(robotId)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found: " + robotId));
        return toStatus(r);
    }

    // Optional helper: status by assisted user id (nice for "elderly user" view)
    @GetMapping("/by-user/{userId}/status")
    public RobotDtos.StatusResponse statusByUser(@PathVariable Long userId) {
        Robot r = robotRepo.findByAssistedUserUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No robot assigned to userId: " + userId));
        return toStatus(r);
    }

    // Status by assisted user's username (admin/caregiver filter)
    @GetMapping("/by-username/{username}/status")
    public RobotDtos.StatusResponse statusByUsername(@PathVariable String username) {
        String u = username == null ? "" : username.trim();
        if (u.isEmpty()) throw new IllegalArgumentException("Username is required.");

        Robot r = robotRepo.findByAssistedUserUserName(u)
                .orElseThrow(() -> new IllegalArgumentException("No robot assigned to username: " + u));

        return toStatus(r);
    }


    /**** ACTIVITIES ****/

    @GetMapping("/{robotId}/activities")
    public List<RobotDtos.ActivityResponse> activities(
            @PathVariable String robotId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        return activityRepo.findByRobot_IdOrderByActivityTimeDesc(robotId, PageRequest.of(0, safeLimit))
                .stream()
                .map(a -> new RobotDtos.ActivityResponse(
                        a.getId(),
                        a.getActivityTime(),
                        a.getActivityType(),
                        a.getSeverity(),
                        a.getActivityDescription()
                ))
                .toList();
    }

    /**** COMMANDS ****/

    @GetMapping("/{robotId}/commands")
    public List<RobotDtos.CommandResponse> commands(
            @PathVariable String robotId,
            @RequestParam(defaultValue = "15") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        return commandRepo.findByRobot_IdOrderByCommandTimeDesc(robotId, PageRequest.of(0, safeLimit))
                .stream()
                .map(c -> new RobotDtos.CommandResponse(
                        c.getId(),
                        c.getCommandTime(),
                        c.getCommandType(),
                        c.getTargetLocation(),
                        c.getDescription(),
                        c.getStatus()
                ))
                .toList();
    }

    @PostMapping("/{robotId}/commands")
    public RobotDtos.CommandResponse issue(
            @PathVariable String robotId,
            @RequestBody RobotDtos.CommandRequest req
    ) {
        var cmd = commandService.issue(robotId, req.commandType(), HouseLocation.valueOf(req.targetLocation()), req.description());

        return new RobotDtos.CommandResponse(
                cmd.getId(),
                cmd.getCommandTime(),
                cmd.getCommandType(),
                cmd.getTargetLocation(),
                cmd.getDescription(),
                cmd.getStatus()
        );
    }

    /** HELPER - Mapping **/

    private RobotDtos.StatusResponse toStatus(Robot r) {
        Long assistedUserId = (r.getAssistedUser() == null) ? null : r.getAssistedUser().getUserId();

        return new RobotDtos.StatusResponse(
                r.getId(),
                r.getRobotName(),
                assistedUserId,
                r.getBatteryLevel(),
                r.getRobotStatus(),
                r.getTrayStatus(),
                r.getSensorStatus(),
                r.getSensorMessage(),
                r.getDispenserStatus(),
                r.getDispenserFillLevel(),
                r.getDispenserPillsRemaining(),
                r.getCurrentLocation(),
                r.getTargetLocation(),
                r.getLastUpdatedAt()
        );
    }
}