package com.azedcods.home_buddy_v2.model.dispenser;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "dispenser_compartment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dispenser_day",
                columnNames = {"dispenser_id", "day_of_month"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DispenserCompartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Keep your existing name to avoid breaking other layers.
     * If you want, later we can rename to dayIndex or dayOfWeek safely with a migration.
     */
    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth;

    @Column(nullable = false)
    private Integer pillsCount = 0;

    @Column(nullable = false)
    private Integer pillCapacity = 7;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispenser_id", nullable = false)
    @ToString.Exclude
    private Dispenser dispenser;

    public DispenserCompartment(Dispenser dispenser, Integer dayOfMonth) {
        this.dispenser = dispenser;
        this.dayOfMonth = dayOfMonth;
        this.pillsCount = 0;
        this.pillCapacity = 7;
    }
}
