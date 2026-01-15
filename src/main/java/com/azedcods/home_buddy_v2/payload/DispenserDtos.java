package com.azedcods.home_buddy_v2.payload;

import java.time.LocalDateTime;
import java.util.List;

public class DispenserDtos {

    public record UpdateRequest(Boolean hasSyrupHolder) {}

    public record CompartmentResponse(
            Long id,
            Integer dayOfMonth,
            Integer pillsCount,
            Integer pillCapacity
    ) {}

    public record Response(
            Long dispenserId,
            String robotId,
            Boolean hasSyrupHolder,
            LocalDateTime lastRefilledAt,
            List<CompartmentResponse> compartments
    ) {}
}
