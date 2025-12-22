package com.azedcods.home_buddy_v2.repository;

import com.azedcods.home_buddy_v2.model.RobotCommand;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RobotCommandRepository extends JpaRepository<RobotCommand, Long> {
    List<RobotCommand> findByOrderByCommandTimeDesc(Pageable pageable);
}
