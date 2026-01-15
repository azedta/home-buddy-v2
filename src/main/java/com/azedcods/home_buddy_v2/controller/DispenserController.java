package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.model.dispenser.Dispenser;
import com.azedcods.home_buddy_v2.model.dispenser.DispenserCompartment;
import com.azedcods.home_buddy_v2.payload.DispenserDtos;
import com.azedcods.home_buddy_v2.service.dispenser.DispenserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispenser")
public class DispenserController {

    private final DispenserService service;

    public DispenserController(DispenserService service) {
        this.service = service;
    }

    /**
     * Fetch dispenser for a robot.
     * Auto-creates if missing.
     */
    @GetMapping("/{robotId}")
    public DispenserDtos.Response get(@PathVariable String robotId) {
        Dispenser d = service.getOrCreateForRobot(robotId);
        List<DispenserCompartment> compartments = service.getCompartments(robotId);
        return toResponse(d, compartments);
    }

    /**
     * Update dispenser config fields (ex: syrup holder toggle).
     */
    @PutMapping("/{robotId}")
    public DispenserDtos.Response update(
            @PathVariable String robotId,
            @Valid @RequestBody DispenserDtos.UpdateRequest req
    ) {
        Dispenser d = service.update(robotId, req);
        List<DispenserCompartment> compartments = service.getCompartments(robotId);
        return toResponse(d, compartments);
    }

    /**
     * Convenience: reset month (empties all compartments).
     */
    @PostMapping("/{robotId}/reset-month")
    public void resetMonth(@PathVariable String robotId) {
        service.resetMonth(robotId);
    }

    // ------------------ Mapping helpers ------------------

    private DispenserDtos.Response toResponse(Dispenser d, List<DispenserCompartment> compartments) {
        return new DispenserDtos.Response(
                d.getId(),
                d.getRobot().getId(),
                d.isHasSyrupHolder(),
                d.getLastRefilledAt(),
                compartments.stream().map(this::toCompartment).toList()
        );
    }

    private DispenserDtos.CompartmentResponse toCompartment(DispenserCompartment c) {
        return new DispenserDtos.CompartmentResponse(
                c.getId(),
                c.getDayOfMonth(),
                c.getPillsCount(),
                c.getPillCapacity()
        );
    }
}
