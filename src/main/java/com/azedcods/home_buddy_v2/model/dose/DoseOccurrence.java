package com.azedcods.home_buddy_v2.model.dose;

import com.azedcods.home_buddy_v2.enums.OccurrenceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
@Table(
        name = "dose_occurrence",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_occurrence_dose_scheduledat",
                        columnNames = {"dose_id", "scheduled_at"}
                )
        },
        indexes = {
                @Index(name = "idx_occurrence_dose_scheduledat", columnList = "dose_id, scheduled_at"),
                @Index(name = "idx_occurrence_status_scheduledat", columnList = "status, scheduled_at")
        }
)
public class DoseOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which dose this occurrence belongs to */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dose_id", nullable = false)
    @ToString.Exclude
    private Dose dose;

    /** Exact time this should happen */
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    /** SCHEDULED, DUE, TAKEN, MISSED, SKIPPED... */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OccurrenceStatus status = OccurrenceStatus.SCHEDULED;

    /** When the user actually took it (if status=TAKEN) */
    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    /** Optional note: "taken late", "felt nauseous", etc */
    @Column(length = 500)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected DoseOccurrence() {}

    public DoseOccurrence(Dose dose, LocalDateTime scheduledAt) {
        this.dose = dose;
        this.scheduledAt = scheduledAt;
        this.status = OccurrenceStatus.SCHEDULED;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = OccurrenceStatus.SCHEDULED;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
