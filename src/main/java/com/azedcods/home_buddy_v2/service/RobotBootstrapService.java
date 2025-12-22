package com.azedcods.home_buddy_v2.service;

import com.azedcods.home_buddy_v2.model.Robot;

import com.azedcods.home_buddy_v2.model.enums.*;
import com.azedcods.home_buddy_v2.repository.RobotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RobotBootstrapService {

    public static final String ROBOT_ID = "LEO-4523124-VAC";

    private final RobotRepository robotRepo;

    public RobotBootstrapService(RobotRepository robotRepo) {
        this.robotRepo = robotRepo;
    }

    @Transactional
    public Robot getOrCreateRobot() {
        return robotRepo.findById(ROBOT_ID).orElseGet(() -> {
            Robot r = new Robot();
            r.setId(ROBOT_ID);
            r.setBatteryLevel(92);
            r.setRobotStatus(RobotStatus.RESTING);
            r.setTrayStatus(TrayStatus.UP);
            r.setSensorStatus(ToggleState.ON);
            r.setSensorMessage("Idle. No motion detected.");
            r.setDispenserStatus(ToggleState.ON);
            r.setDispenserPillsRemaining(18);
            r.setDispenserFillLevel(FillLevel.FULL);
            r.setCurrentLocation("DOCK");
            r.setTargetLocation("DOCK");
            r.setLastUpdatedAt(Instant.now());
            return robotRepo.save(r);
        });
    }
}
