package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.model.Robot;
import com.azedcods.home_buddy_v2.payload.RobotDtos;
import com.azedcods.home_buddy_v2.repository.RobotActivityRepository;
import com.azedcods.home_buddy_v2.repository.RobotCommandRepository;
import com.azedcods.home_buddy_v2.repository.RobotRepository;
import com.azedcods.home_buddy_v2.service.RobotBootstrapService;
import com.azedcods.home_buddy_v2.service.RobotCommandService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/robot")
public class RobotController {

    private final RobotBootstrapService bootstrap;
    private final RobotRepository robotRepo;
    private final RobotActivityRepository activityRepo;
    private final RobotCommandRepository commandRepo;
    private final RobotCommandService commandService;

    public RobotController(
            RobotBootstrapService bootstrap,
            RobotRepository robotRepo,
            RobotActivityRepository activityRepo,
            RobotCommandRepository commandRepo,
            RobotCommandService commandService
    ) {
        this.bootstrap = bootstrap;
        this.robotRepo = robotRepo;
        this.activityRepo = activityRepo;
        this.commandRepo = commandRepo;
        this.commandService = commandService;
    }

    @GetMapping("/status")
    public RobotDtos.StatusResponse status() {
        Robot r = bootstrap.getOrCreateRobot();
        return new RobotDtos.StatusResponse(
                r.getId(),
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

    @GetMapping("/activities")
    public List<RobotDtos.ActivityResponse> activities(@RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return activityRepo.findByOrderByActivityTimeDesc(PageRequest.of(0, safeLimit))
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

    @GetMapping("/commands")
    public List<RobotDtos.CommandResponse> commands(@RequestParam(defaultValue = "15") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return commandRepo.findByOrderByCommandTimeDesc(PageRequest.of(0, safeLimit))
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

    @PostMapping("/commands")
    public RobotDtos.CommandResponse issue(@RequestBody RobotDtos.CommandRequest req) {
        var cmd = commandService.issue(req.commandType(), req.targetLocation(), req.description());
        return new RobotDtos.CommandResponse(
                cmd.getId(),
                cmd.getCommandTime(),
                cmd.getCommandType(),
                cmd.getTargetLocation(),
                cmd.getDescription(),
                cmd.getStatus()
        );
    }
}