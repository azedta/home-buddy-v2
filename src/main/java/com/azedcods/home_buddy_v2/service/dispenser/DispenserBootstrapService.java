package com.azedcods.home_buddy_v2.service.dispenser;

import com.azedcods.home_buddy_v2.model.dispenser.Dispenser;
import com.azedcods.home_buddy_v2.model.dispenser.DispenserCompartment;
import com.azedcods.home_buddy_v2.model.robot.Robot;
import com.azedcods.home_buddy_v2.repository.dispenser.DispenserCompartmentRepository;
import com.azedcods.home_buddy_v2.repository.dispenser.DispenserRepository;
import com.azedcods.home_buddy_v2.repository.robot.RobotRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DispenserBootstrapService {

    private static final int DEFAULT_COMPARTMENTS = 31;

    private final DispenserRepository dispenserRepo;
    private final DispenserCompartmentRepository compartmentRepo;
    private final RobotRepository robotRepo;

    public DispenserBootstrapService(
            DispenserRepository dispenserRepo,
            DispenserCompartmentRepository compartmentRepo,
            RobotRepository robotRepo
    ) {
        this.dispenserRepo = dispenserRepo;
        this.compartmentRepo = compartmentRepo;
        this.robotRepo = robotRepo;
    }

    /**
     * Ensure a dispenser exists for a robot (idempotent).
     * Safe to call multiple times.
     *
     * - Creates Dispenser if missing
     * - Ensures 31 compartments exist (fills missing ones)
     */
    public Dispenser ensureForRobot(String robotId) {
        Robot robot = robotRepo.findById(robotId)
                .orElseThrow(() -> new EntityNotFoundException("Robot not found: " + robotId));

        Dispenser dispenser = dispenserRepo.findByRobot_Id(robotId)
                .orElseGet(() -> createEmptyDispenser(robot));

        ensureCompartments(dispenser, DEFAULT_COMPARTMENTS);

        return dispenser;
    }

    private Dispenser createEmptyDispenser(Robot robot) {
        Dispenser d = new Dispenser(robot);
        d.setHasSyrupHolder(true);
        d.setLastRefilledAt(LocalDateTime.now());
        return dispenserRepo.save(d);
    }

    private void ensureCompartments(Dispenser dispenser, int count) {
        // Load existing compartments (if any)
        List<DispenserCompartment> existing =
                compartmentRepo.findByDispenser_IdOrderByDayOfMonthAsc(dispenser.getId());

        Set<Integer> existingDays = new HashSet<>();
        for (DispenserCompartment c : existing) {
            if (c.getDayOfMonth() != null) existingDays.add(c.getDayOfMonth());
        }

        // Create missing compartments (1..count)
        boolean createdAny = false;
        for (int day = 1; day <= count; day++) {
            if (!existingDays.contains(day)) {
                // Keep relationship consistent
                dispenser.addCompartment(new DispenserCompartment(dispenser, day));
                createdAny = true;
            }
        }

        // Save only if we actually added new compartments
        if (createdAny) {
            dispenserRepo.save(dispenser);
        }
    }
}
