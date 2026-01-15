package com.azedcods.home_buddy_v2.model.dose;

import com.azedcods.home_buddy_v2.model.auth.User;
import com.azedcods.home_buddy_v2.enums.DoseUnit;
import com.azedcods.home_buddy_v2.model.medication.Medication;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(
        name = "dose",
        indexes = {
                @Index(name = "idx_dose_user", columnList = "user_id"),
                @Index(name = "idx_dose_medication", columnList = "medication_id")
        }
)
public class Dose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** How many times per day (e.g., 1,2,3...) */
    @Column(nullable = false)
    private Integer timeFrequency;

    /**
     * Which days the dose is active.
     * Example: MON…SUN for daily, or only MON/WED/FRI, etc.
     * Empty set can mean "every day" depending on your engine choice.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dose_days_of_week", joinColumns = @JoinColumn(name = "dose_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private Set<DayOfWeek> daysOfWeek = new LinkedHashSet<>();

    /**
     * Times in the day, size should match timeFrequency.
     * Example: [07:00, 14:00] if timeFrequency = 2
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dose_times", joinColumns = @JoinColumn(name = "dose_id"))
    @Column(name = "time_of_day", nullable = false)
    private Set<LocalTime> times = new LinkedHashSet<>();

    /** Quantity amount (e.g., 2, 1.5) */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityAmount;

    /** Quantity unit (PILL, CAPSULE, TABLESPOON, ML...) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DoseUnit quantityUnit;

    /** Optional: schedule start/end */
    private LocalDate startDate;
    private LocalDate endDate;

    /** Optional: notes like "after meals", "with water" */
    @Column(length = 500)
    private String instructions;

    /** Medication relationship: each medication can have many doses */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    /** User relationship: patient who should take these doses */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Optional but useful
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Dose() {}

    public Dose(
            Integer timeFrequency,
            Set<DayOfWeek> daysOfWeek,
            Set<LocalTime> times,
            BigDecimal quantityAmount,
            DoseUnit quantityUnit,
            LocalDate startDate,
            LocalDate endDate,
            String instructions,
            Medication medication,
            User user
    ) {
        this.timeFrequency = timeFrequency;
        this.daysOfWeek = daysOfWeek;
        this.times = times;
        this.quantityAmount = quantityAmount;
        this.quantityUnit = quantityUnit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructions = instructions;
        this.medication = medication;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.startDate == null) this.startDate = LocalDate.now();
        if (this.timeFrequency == null || this.timeFrequency < 1) this.timeFrequency = 1;
        if (this.quantityAmount == null) this.quantityAmount = BigDecimal.ONE;
        if (this.quantityUnit == null) this.quantityUnit = DoseUnit.PILL;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
