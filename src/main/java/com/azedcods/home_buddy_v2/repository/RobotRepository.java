package com.azedcods.home_buddy_v2.repository;

import com.azedcods.home_buddy_v2.model.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotRepository extends JpaRepository<Robot, String> { }

