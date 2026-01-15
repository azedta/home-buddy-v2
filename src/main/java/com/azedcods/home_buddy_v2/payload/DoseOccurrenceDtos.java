package com.azedcods.home_buddy_v2.payload;

import com.azedcods.home_buddy_v2.enums.OccurrenceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class DoseOccurrenceDtos {

    public record Response(
            Long id,
            Long doseId,
            LocalDateTime scheduledAt,
            OccurrenceStatus status,
            LocalDateTime takenAt,
            String note
    ) {}

    public record MarkTakenRequest(
            @NotNull LocalDateTime takenAt,
            String note
    ) {}

    public record MarkStatusRequest(
            @NotNull OccurrenceStatus status,
            String note
    ) {}

    public record GenerateRequest(
            @NotNull LocalDateTime from,
            @NotNull LocalDateTime to
    ) {}

    public record WindowResponse(
            LocalDateTime from,
            LocalDateTime to,
            List<Response> items
    ) {}
}
