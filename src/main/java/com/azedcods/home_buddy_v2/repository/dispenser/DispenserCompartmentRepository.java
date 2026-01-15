package com.azedcods.home_buddy_v2.repository.dispenser;

import com.azedcods.home_buddy_v2.model.dispenser.DispenserCompartment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DispenserCompartmentRepository extends JpaRepository<DispenserCompartment, Long> {

    // ✅ needed by DispenserBootstrapService (your error)
    List<DispenserCompartment> findByDispenser_IdOrderByDayOfMonthAsc(Long dispenserId);

    // ✅ used by DispenserService + jobs
    List<DispenserCompartment> findByDispenser_Robot_IdOrderByDayOfMonthAsc(String robotId);

    Optional<DispenserCompartment> findByDispenser_Robot_IdAndDayOfMonth(String robotId, Integer dayOfMonth);

    @Query("""
            select coalesce(sum(c.pillsCount), 0)
            from DispenserCompartment c
            where c.dispenser.robot.id = :robotId
            """)
    long sumPillsForRobot(@Param("robotId") String robotId);
}
