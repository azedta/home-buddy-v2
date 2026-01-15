package com.azedcods.home_buddy_v2.model.dose;

import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;

@Component
public class DefaultScheduleEngine implements ScheduleEngine {

    @Override
    public List<LocalDateTime> computeSchedule(Dose dose, LocalDateTime from, LocalDateTime to) {
        if (dose == null || from == null || to == null) return List.of();
        if (to.isBefore(from)) return List.of();

        Integer freqPerDay = dose.getTimeFrequency();
        Set<DayOfWeek> days = dose.getDaysOfWeek();
        Set<LocalTime> timesSet = dose.getTimes();

        // Clamp window by dose start/end
        LocalDateTime windowFrom = from;
        LocalDateTime windowTo = to;

        if (dose.getStartDate() != null) {
            windowFrom = max(windowFrom, dose.getStartDate().atStartOfDay());
        }
        if (dose.getEndDate() != null) {
            windowTo = min(windowTo, dose.getEndDate().atTime(LocalTime.MAX));
        }
        if (windowTo.isBefore(windowFrom)) return List.of();

        List<LocalTime> times = resolveTimes(timesSet, freqPerDay);

        List<LocalDateTime> results = new ArrayList<>();
        LocalDate d = windowFrom.toLocalDate();
        LocalDate end = windowTo.toLocalDate();

        while (!d.isAfter(end)) {
            if (shouldIncludeDay(days, d.getDayOfWeek())) {
                for (LocalTime t : times) {
                    LocalDateTime dt = LocalDateTime.of(d, t);
                    if (!dt.isBefore(windowFrom) && !dt.isAfter(windowTo)) {
                        results.add(dt);
                    }
                }
            }
            d = d.plusDays(1);
        }

        results.sort(Comparator.naturalOrder());
        return results;
    }

    private boolean shouldIncludeDay(Set<DayOfWeek> days, DayOfWeek day) {
        return days == null || days.isEmpty() || days.contains(day);
    }

    private List<LocalTime> resolveTimes(Set<LocalTime> timesSet, Integer freqPerDay) {
        // Use explicit times if provided
        if (timesSet != null && !timesSet.isEmpty()) {
            return timesSet.stream().distinct().sorted().toList();
        }

        int f = (freqPerDay == null || freqPerDay < 1) ? 1 : Math.min(freqPerDay, 24);

        // Human-friendly defaults for common frequencies
        if (f <= 6) {
            return switch (f) {
                case 2 -> List.of(LocalTime.of(9, 0), LocalTime.of(21, 0));
                case 3 -> List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));
                case 4 -> List.of(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0));
                case 5 -> List.of(LocalTime.of(7, 0), LocalTime.of(11, 0), LocalTime.of(15, 0), LocalTime.of(19, 0), LocalTime.of(22, 0));
                case 6 -> List.of(LocalTime.of(6, 0), LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(18, 0), LocalTime.of(21, 0), LocalTime.of(23, 0));
                default -> List.of(LocalTime.of(9, 0));
            };
        }

        // For high frequencies: space times between 06:00 and 22:00
        // (avoids scheduling at 03:00 etc.)
        int startHour = 6;
        int endHour = 22;

        List<LocalTime> out = new ArrayList<>(f);
        double step = (endHour - startHour) / (double) (f - 1);

        for (int i = 0; i < f; i++) {
            int hour = (int) Math.round(startHour + i * step);
            hour = Math.max(0, Math.min(23, hour));
            out.add(LocalTime.of(hour, 0));
        }

        return out.stream().distinct().sorted().toList();
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
