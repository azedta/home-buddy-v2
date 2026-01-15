package com.azedcods.home_buddy_v2.model.dose;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleEngine {

    /**
     * Produce scheduled datetimes for the given dose within [from, to] inclusive-ish.
     * The service layer will handle persistence + de-duplication.
     */
    List<LocalDateTime> computeSchedule(Dose dose, LocalDateTime from, LocalDateTime to);

    /**
     * Helper: clamp date range to day boundaries.
     */
    default LocalDate startDay(LocalDateTime from) {
        return from.toLocalDate();
    }
}
