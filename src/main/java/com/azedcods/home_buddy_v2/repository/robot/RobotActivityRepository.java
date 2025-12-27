package com.azedcods.home_buddy_v2.repository;

import com.azedcods.home_buddy_v2.model.robot.RobotActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RobotActivityRepository extends JpaRepository<RobotActivity, Long> {

    List<RobotActivity> findByRobot_IdOrderByActivityTimeDesc(String robotId, Pageable pageable);

    List<RobotActivity> findByRobot_IdOrderByActivityTimeAsc(String robotId, Pageable pageable);

    long countByRobot_Id(String robotId);
}
