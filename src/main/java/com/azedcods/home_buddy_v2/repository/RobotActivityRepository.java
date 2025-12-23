package com.azedcods.home_buddy_v2.repository;

import com.azedcods.home_buddy_v2.model.robot.RobotActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RobotActivityRepository extends JpaRepository<RobotActivity, Long> {

    List<RobotActivity> findByOrderByActivityTimeDesc(Pageable pageable);

    List<RobotActivity> findByOrderByActivityTimeAsc(Pageable pageable);

    long count();
}