package com.azedcods.home_buddy_v2.service.notification;

import com.azedcods.home_buddy_v2.enums.*;
import com.azedcods.home_buddy_v2.model.dose.DoseOccurrence;
import com.azedcods.home_buddy_v2.repository.dose.DoseOccurrenceRepository;
import com.azedcods.home_buddy_v2.service.dose.DoseOccurrenceService;
import com.azedcods.home_buddy_v2.service.notification.NotificationEngine;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicationReminderJob {

    private final DoseOccurrenceService doseOccurrenceService;
    private final DoseOccurrenceRepository occRepo;
    private final NotificationEngine notificationEngine;

    // tune these later
    private static final int DUE_LOOKBACK_MIN = 2;      // catch items that became due in the last 2 minutes
    private static final int DUE_LOOKAHEAD_MIN = 1;     // tiny lookahead for clock drift
    private static final int CONFIRM_AFTER_MIN = 15;    // ask confirmation after 15 minutes overdue

    public MedicationReminderJob(
            DoseOccurrenceService doseOccurrenceService,
            DoseOccurrenceRepository occRepo,
            NotificationEngine notificationEngine
    ) {
        this.doseOccurrenceService = doseOccurrenceService;
        this.occRepo = occRepo;
        this.notificationEngine = notificationEngine;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void tickMedicationNotifications() {
        LocalDateTime now = LocalDateTime.now();

        // Keep DUE/MISSED statuses current (you already have this logic)
        doseOccurrenceService.refreshDueAndMissed(now);

        emitDoseDue(now);
        emitConfirmRequired(now);
        emitDoseMissed(now);
    }

    private void emitDoseDue(LocalDateTime now) {
        LocalDateTime from = now.minusMinutes(DUE_LOOKBACK_MIN);
        LocalDateTime to = now.plusMinutes(DUE_LOOKAHEAD_MIN);

        List<DoseOccurrence> due = occRepo.findByStatusAndTakenAtIsNullAndScheduledAtBetween(
                OccurrenceStatus.DUE, from, to
        );

        for (DoseOccurrence o : due) {
            Long userId = o.getDose().getUser().getUserId();
            String key = "DOSE_DUE:occ=" + o.getId() + ":user=" + userId;

            notificationEngine.emit(
                    NotificationRule.DOSE_DUE,
                    userId,
                    key,
                    NotificationType.MEDICATION,
                    NotificationSeverity.INFO,
                    "Medication due now",
                    "Dose #" + o.getId() + " is due (scheduled at " + o.getScheduledAt() + ").",
                    "/medication?tab=schedule",
                    Duration.ofMinutes(30)
            );
        }
    }

    private void emitConfirmRequired(LocalDateTime now) {
        // Anything still DUE and overdue by 15 minutes
        LocalDateTime cutoff = now.minusMinutes(CONFIRM_AFTER_MIN);

        List<DoseOccurrence> overdue = occRepo.findByStatusAndTakenAtIsNullAndScheduledAtLessThanEqual(
                OccurrenceStatus.DUE, cutoff
        );

        for (DoseOccurrence o : overdue) {
            Long userId = o.getDose().getUser().getUserId();
            String key = "DOSE_CONFIRM_REQUIRED:occ=" + o.getId() + ":user=" + userId;

            notificationEngine.emit(
                    NotificationRule.DOSE_CONFIRM_REQUIRED,
                    userId,
                    key,
                    NotificationType.MEDICATION,
                    NotificationSeverity.WARN,
                    "Please confirm your medication",
                    "Dose #" + o.getId() + " scheduled at " + o.getScheduledAt() + " is still not marked as taken.",
                    "/medication?tab=schedule",
                    Duration.ofMinutes(60)
            );
        }
    }

    private void emitDoseMissed(LocalDateTime now) {
        // Your system marks MISSED after 24 hours. Emit it once when it becomes MISSED.
        // We'll scan a reasonable window (last 48h scheduled) to avoid touching ancient history.
        LocalDateTime from = now.minusHours(48);
        LocalDateTime to = now;

        List<DoseOccurrence> missed = occRepo.findByStatusAndTakenAtIsNullAndScheduledAtBetween(
                OccurrenceStatus.MISSED, from, to
        );

        for (DoseOccurrence o : missed) {
            Long userId = o.getDose().getUser().getUserId();
            String key = "DOSE_MISSED:occ=" + o.getId() + ":user=" + userId;

            notificationEngine.emit(
                    NotificationRule.DOSE_MISSED,
                    userId,
                    key,
                    NotificationType.MEDICATION,
                    NotificationSeverity.CRITICAL,
                    "Medication missed",
                    "Dose #" + o.getId() + " scheduled at " + o.getScheduledAt() + " was missed.",
                    "/medication?tab=schedule",
                    Duration.ofDays(7) // basically “once”
            );
        }
    }
}
