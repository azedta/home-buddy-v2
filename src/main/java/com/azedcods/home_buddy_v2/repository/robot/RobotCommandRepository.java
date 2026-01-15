package com.azedcods.home_buddy_v2.repository.robot;

import com.azedcods.home_buddy_v2.model.robot.RobotCommand;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RobotCommandRepository extends JpaRepository<RobotCommand, Long> {

    List<RobotCommand> findByRobot_IdOrderByCommandTimeDesc(String robotId, Pageable pageable);

    long countByRobot_Id(String robotId);
}
