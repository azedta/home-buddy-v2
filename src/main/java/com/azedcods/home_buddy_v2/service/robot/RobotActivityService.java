package com.azedcods.home_buddy_v2.service.robot;

import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.model.robot.RobotActivity;
import com.azedcods.home_buddy_v2.model.enums.*;
import com.azedcods.home_buddy_v2.repository.RobotActivityRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RobotActivityService {

    private static final int MAX_ACTIVITIES = 100;

    private final RobotActivityRepository activityRepo;

    public RobotActivityService(RobotActivityRepository activityRepo) {
        this.activityRepo = activityRepo;
    }

    @Transactional
    public RobotActivity log(Robot robot, ActivityType type, ActivitySeverity severity, String desc) {
        RobotActivity a = new RobotActivity();
        a.setRobot(robot);
        a.setActivityTime(Instant.now());
        a.setActivityType(type);
        a.setSeverity(severity);
        a.setActivityDescription(desc);

        RobotActivity saved = activityRepo.save(a);
        trimToMax();
        return saved;
    }

    @Transactional
    public void trimToMax() {
        long count = activityRepo.count();
        if (count <= MAX_ACTIVITIES) return;

        int overflow = (int) (count - MAX_ACTIVITIES);
        List<RobotActivity> oldest = activityRepo.findByOrderByActivityTimeAsc(PageRequest.of(0, overflow));
        activityRepo.deleteAll(oldest);
    }
}
