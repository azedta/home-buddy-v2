package com.azedcods.home_buddy_v2.service.dose;

import com.azedcods.home_buddy_v2.enums.OccurrenceStatus;
import com.azedcods.home_buddy_v2.enums.NotificationRule;
import com.azedcods.home_buddy_v2.enums.NotificationSeverity;
import com.azedcods.home_buddy_v2.enums.NotificationType;
import com.azedcods.home_buddy_v2.model.dose.Dose;
import com.azedcods.home_buddy_v2.model.dose.DoseOccurrence;
import com.azedcods.home_buddy_v2.model.dose.ScheduleEngine;
import com.azedcods.home_buddy_v2.repository.dose.DoseOccurrenceRepository;
import com.azedcods.home_buddy_v2.repository.dose.DoseRepository;
import com.azedcods.home_buddy_v2.service.dispenser.DispenserService;
import com.azedcods.home_buddy_v2.service.notification.NotificationEngine;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoseOccurrenceService {

    private static final int MAX_OCCURRENCES_PER_DAY = 7;


    private final DoseRepository doseRepo;
    private final DoseOccurrenceRepository occRepo;
    private final ScheduleEngine scheduleEngine;
    private final DispenserService dispenserService;
    private final NotificationEngine notificationEngine;

    public DoseOccurrenceService(
            DoseRepository doseRepo,
            DoseOccurrenceRepository occRepo,
            ScheduleEngine scheduleEngine,
            DispenserService dispenserService,
            NotificationEngine notificationEngine
    ) {
        this.doseRepo = doseRepo;
        this.occRepo = occRepo;
        this.scheduleEngine = scheduleEngine;
        this.dispenserService = dispenserService;
        this.notificationEngine = notificationEngine;
    }

    @Transactional
    public List<DoseOccurrence> listWindowForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (from == null || to == null) throw new IllegalArgumentException("from/to are required");

        refreshDueAndMissed(LocalDateTime.now());

        return occRepo.findByDose_User_UserIdAndScheduledAtBetweenOrderByScheduledAtAsc(userId, from, to);
    }

    public DoseOccurrence get(Long id) {
        return occRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Occurrence not found: " + id));
    }

    @Transactional
    public List<DoseOccurrence> generateWindowForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (from == null || to == null) throw new IllegalArgumentException("from/to are required");

        // ✅ Load existing occurrences once (whole window)
        List<DoseOccurrence> existingOcc =
                occRepo.findByDose_User_UserIdAndScheduledAtBetweenOrderByScheduledAtAsc(userId, from, to);

        // ✅ date -> count (use LocalDate, not dayOfMonth; avoids cross-month collisions)
        Map<java.time.LocalDate, Integer> dateCount = new java.util.HashMap<>();
        for (DoseOccurrence o : existingOcc) {
            if (o.getScheduledAt() == null) continue;
            dateCount.merge(o.getScheduledAt().toLocalDate(), 1, Integer::sum);
        }

        List<Dose> doses = doseRepo.findByUser_UserId(userId);

        // ✅ We validate ALL first, collect violations, and only save if valid
        List<DoseOccurrence> toCreateAll = new java.util.ArrayList<>();
        List<CapacityViolation> violations = new java.util.ArrayList<>();

        for (Dose d : doses) {
            List<LocalDateTime> scheduled = scheduleEngine.computeSchedule(d, from, to);

            var existingTimesForDose = new java.util.HashSet<>(
                    occRepo.findExistingTimesForDoseInRange(d.getId(), from, to)
            );

            for (LocalDateTime at : scheduled) {
                if (existingTimesForDose.contains(at)) continue;

                java.time.LocalDate date = at.toLocalDate();
                int current = dateCount.getOrDefault(date, 0);

                if (current >= MAX_OCCURRENCES_PER_DAY) {
                    violations.add(new CapacityViolation(at, d.getId(), current));
                    continue;
                }

                // Tentatively accept + increment in-memory
                dateCount.put(date, current + 1);

                toCreateAll.add(DoseOccurrence.builder()
                        .dose(d)
                        .scheduledAt(at)
                        .status(OccurrenceStatus.SCHEDULED)
                        .build());
            }
        }

        // ✅ STRICT: if anything violates capacity, we throw a detailed report
        if (!violations.isEmpty()) {
            // Group violations by date for a cleaner message
            Map<java.time.LocalDate, List<CapacityViolation>> byDate = new java.util.LinkedHashMap<>();
            for (CapacityViolation v : violations) {
                byDate.computeIfAbsent(v.at.toLocalDate(), k -> new java.util.ArrayList<>()).add(v);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Cannot generate occurrences: dispenser capacity is ")
                    .append(MAX_OCCURRENCES_PER_DAY)
                    .append(" per day.\n\n");

            int shown = 0;
            int MAX_LINES = 25; // prevent insane error messages

            for (var entry : byDate.entrySet()) {
                java.time.LocalDate date = entry.getKey();
                List<CapacityViolation> items = entry.getValue();

                // the first violation’s existingCount reflects "already full"
                int existing = items.get(0).existingCount;

                sb.append("• ").append(date)
                        .append(" already has ").append(existing)
                        .append(" scheduled occurrence(s). Attempted to add ")
                        .append(items.size()).append(" more.\n");

                for (CapacityViolation v : items) {
                    if (shown >= MAX_LINES) break;
                    sb.append("   - doseId=").append(v.doseId)
                            .append(" at ").append(String.format("%02d:%02d", v.at.getHour(), v.at.getMinute()))
                            .append("\n");
                    shown++;
                }

                if (shown >= MAX_LINES) {
                    sb.append("\n(Only first ").append(MAX_LINES).append(" overflow items shown.)\n");
                    break;
                }
            }

            sb.append("\nFix: reduce daily frequency / remove overlapping doses / adjust schedule rules.");

            throw new IllegalStateException(sb.toString());
        }

        // ✅ Save only if valid
        if (!toCreateAll.isEmpty()) {
            occRepo.saveAll(toCreateAll);
        }

        refreshDueAndMissed(LocalDateTime.now());

        // ✅ Keep your dispenser sync (idempotent and now guaranteed consistent)
        syncDispenserForUserWindow(userId, from, to);

        return occRepo.findByDose_User_UserIdAndScheduledAtBetweenOrderByScheduledAtAsc(userId, from, to);
    }



    private void syncDispenserForUserWindow(Long userId, LocalDateTime from, LocalDateTime to) {
        String robotId = dispenserService.resolveRobotIdForUser(userId);
        if (robotId == null) return;

        List<DoseOccurrence> occurrences =
                occRepo.findByDose_User_UserIdAndScheduledAtBetweenOrderByScheduledAtAsc(userId, from, to);

        Map<Integer, Integer> pillsByDay = new HashMap<>();
        for (DoseOccurrence o : occurrences) {
            if (o.getScheduledAt() == null) continue;
            pillsByDay.merge(o.getScheduledAt().getDayOfMonth(), 1, Integer::sum);
        }

        dispenserService.applyDayLoad(robotId, pillsByDay);
    }

    @Transactional
    public void refreshDueAndMissed(LocalDateTime now) {
        if (now == null) now = LocalDateTime.now();

        occRepo.bumpScheduledToDue(now, "[AUTO] Status → DUE at " + now);
        occRepo.bumpDueToMissed(now.minusHours(24), "[AUTO] Status → MISSED at " + now);
    }

    private void assertUpdatableNow(DoseOccurrence o, LocalDateTime now) {
        if (now == null) now = LocalDateTime.now();

        if (o.getScheduledAt() == null) throw new IllegalStateException("Occurrence scheduledAt is null");

        if (o.getScheduledAt().isAfter(now)) {
            throw new IllegalArgumentException("Cannot update a future occurrence.");
        }

        if (o.getScheduledAt().isBefore(now.minusHours(24))) {
            throw new IllegalArgumentException("Occurrence is locked (more than 24h overdue).");
        }

        if (o.getStatus() == OccurrenceStatus.MISSED) {
            throw new IllegalArgumentException("Occurrence already MISSED and locked.");
        }

        // ✅ NEW: prevent double-taken (prevents double dispense)
        if (o.getStatus() == OccurrenceStatus.TAKEN) {
            throw new IllegalArgumentException("Occurrence already TAKEN and locked.");
        }
    }

    @Transactional
    public DoseOccurrence markTaken(Long occurrenceId, LocalDateTime takenAt, String note) {
        LocalDateTime now = LocalDateTime.now();

        refreshDueAndMissed(now);

        DoseOccurrence o = get(occurrenceId);
        assertUpdatableNow(o, now);

        o.setStatus(OccurrenceStatus.TAKEN);
        o.setTakenAt(takenAt != null ? takenAt : now);
        if (note != null && !note.isBlank()) o.setNote(note.trim());

        Long userId = o.getDose().getUser().getUserId();

        // ✅ NEW: dispense 1 pill from the scheduled day compartment
        String robotId = dispenserService.resolveRobotIdForUser(userId);
        if (robotId != null) {
            dispenserService.dispenseForScheduledTime(robotId, o.getScheduledAt());
        }

        notificationEngine.emit(
                NotificationRule.DOSE_TAKEN,
                userId,
                "DOSE_TAKEN:occ=" + o.getId() + ":user=" + userId,
                NotificationType.MEDICATION,
                NotificationSeverity.SUCCESS,
                "Medication marked as taken",
                "Dose #" + o.getId() + " scheduled at " + o.getScheduledAt() + " was marked as taken.",
                "/medication?tab=schedule",
                Duration.ofMinutes(5)
        );

        return o;
    }

    @Transactional
    public DoseOccurrence setStatus(Long occurrenceId, OccurrenceStatus status, String note) {
        if (status == null) throw new IllegalArgumentException("status is required");
        if (status != OccurrenceStatus.TAKEN && status != OccurrenceStatus.MISSED) {
            throw new IllegalArgumentException("Only TAKEN or MISSED are allowed.");
        }

        LocalDateTime now = LocalDateTime.now();
        refreshDueAndMissed(now);

        DoseOccurrence o = get(occurrenceId);

        // If setting TAKEN, enforce the “no double-dispense” lock
        if (status == OccurrenceStatus.TAKEN) {
            assertUpdatableNow(o, now);
        } else {
            // MISSED also should not flip a TAKEN back
            if (o.getStatus() == OccurrenceStatus.TAKEN) {
                throw new IllegalArgumentException("Occurrence already TAKEN and locked.");
            }
        }

        o.setStatus(status);
        if (note != null && !note.isBlank()) o.setNote(note.trim());

        if (status != OccurrenceStatus.TAKEN) {
            o.setTakenAt(null);
        } else if (o.getTakenAt() == null) {
            o.setTakenAt(now);
        }

        Long userId = o.getDose().getUser().getUserId();

        if (status == OccurrenceStatus.TAKEN) {
            String robotId = dispenserService.resolveRobotIdForUser(userId);
            if (robotId != null) {
                dispenserService.dispenseForScheduledTime(robotId, o.getScheduledAt());
            }
        }

        return o;
    }

    private static String fmt(LocalDateTime dt) {
        if (dt == null) return "null";
        return dt.toLocalDate() + " " + String.format("%02d:%02d", dt.getHour(), dt.getMinute());
    }

    private static class CapacityViolation {
        final LocalDateTime at;
        final Long doseId;
        final int existingCount;

        CapacityViolation(LocalDateTime at, Long doseId, int existingCount) {
            this.at = at;
            this.doseId = doseId;
            this.existingCount = existingCount;
        }
    }

}
