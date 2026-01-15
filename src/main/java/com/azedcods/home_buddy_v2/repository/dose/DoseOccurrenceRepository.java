package com.azedcods.home_buddy_v2.repository.dose;

import com.azedcods.home_buddy_v2.enums.OccurrenceStatus;
import com.azedcods.home_buddy_v2.model.dose.DoseOccurrence;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DoseOccurrenceRepository extends JpaRepository<DoseOccurrence, Long> {

    // existing methods you already use:
    List<DoseOccurrence> findByDose_User_UserIdAndScheduledAtBetweenOrderByScheduledAtAsc(
            Long userId, LocalDateTime from, LocalDateTime to
    );

    @Query("""
            select o.scheduledAt from DoseOccurrence o
            where o.dose.id = :doseId
              and o.scheduledAt between :from and :to
            """)
    List<LocalDateTime> findExistingTimesForDoseInRange(
            @Param("doseId") Long doseId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Modifying
    @Query("""
            update DoseOccurrence o
               set o.status = com.azedcods.home_buddy_v2.enums.OccurrenceStatus.DUE,
                   o.note = :note
             where o.status = com.azedcods.home_buddy_v2.enums.OccurrenceStatus.SCHEDULED
               and o.scheduledAt <= :now
            """)
    int bumpScheduledToDue(@Param("now") LocalDateTime now, @Param("note") String note);

    @Modifying
    @Query("""
            update DoseOccurrence o
               set o.status = com.azedcods.home_buddy_v2.enums.OccurrenceStatus.MISSED,
                   o.note = :note
             where o.status = com.azedcods.home_buddy_v2.enums.OccurrenceStatus.DUE
               and o.takenAt is null
               and o.scheduledAt <= :missedCutoff
            """)
    int bumpDueToMissed(@Param("missedCutoff") LocalDateTime missedCutoff, @Param("note") String note);

    // ✅ NEW: used by MedicationReminderJob
    @EntityGraph(attributePaths = {"dose", "dose.user"})
    List<DoseOccurrence> findByStatusAndTakenAtIsNullAndScheduledAtBetween(
            OccurrenceStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    @EntityGraph(attributePaths = {"dose", "dose.user"})
    List<DoseOccurrence> findByStatusAndTakenAtIsNullAndScheduledAtLessThanEqual(
            OccurrenceStatus status,
            LocalDateTime to
    );
}
