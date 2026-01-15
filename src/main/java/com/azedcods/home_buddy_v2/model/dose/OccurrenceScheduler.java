package com.azedcods.home_buddy_v2.model.dose;

import com.azedcods.home_buddy_v2.service.dose.DoseOccurrenceService;
import com.azedcods.home_buddy_v2.repository.dose.DoseRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OccurrenceScheduler {

    private final DoseOccurrenceService occurrenceService;
    private final DoseRepository doseRepo;

    public OccurrenceScheduler(DoseOccurrenceService occurrenceService, DoseRepository doseRepo) {
        this.occurrenceService = occurrenceService;
        this.doseRepo = doseRepo;
    }

    // Every 15 minutes: update statuses (SCHEDULED->DUE, DUE->MISSED)
    @Scheduled(cron = "0 */15 * * * *")
    public void refreshDueMissed() {
        occurrenceService.refreshDueAndMissed(LocalDateTime.now());
    }

    // Every day at 02:10: generate next 7 days for EVERY user who has doses
    @Scheduled(cron = "0 10 2 * * *")
    public void generateNextWeekForAllUsers() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(7);

        List<Long> userIds = doseRepo.findDistinctUserIdsWithDoses();
        for (Long userId : userIds) {
            occurrenceService.generateWindowForUser(userId, from, to);
        }
    }
}
