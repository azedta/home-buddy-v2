package com.azedcods.home_buddy_v2.repository.dispenser;

import com.azedcods.home_buddy_v2.model.dispenser.Dispenser;
import com.azedcods.home_buddy_v2.model.dispenser.DispenserCompartment;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface DispenserRepository extends JpaRepository<Dispenser, Long> {

    Optional<Dispenser> findByRobot_Id(String robotId);

    // ✅ used by DispenserHealthJob
    @EntityGraph(attributePaths = {"robot", "robot.assistedUser"})
    List<Dispenser> findAll();
}
