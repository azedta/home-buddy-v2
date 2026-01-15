package com.azedcods.home_buddy_v2.repository.robot;

import com.azedcods.home_buddy_v2.model.robot.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, String> {

    Optional<Robot> findByAssistedUserUserId(Long userId);

    boolean existsByAssistedUserUserId(Long userId);

    Optional<Robot> findByAssistedUserUserName(String userName);
}
