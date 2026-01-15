package com.azedcods.home_buddy_v2.model.dispenser;

import com.azedcods.home_buddy_v2.model.robot.Robot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "dispenser",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dispenser_robot", columnNames = {"robot_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Dispenser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 1 Robot <-> 1 Dispenser
     * Many dispensers can exist in DB, but each robot can have only one.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "robot_id", nullable = false, unique = true)
    @ToString.Exclude
    private Robot robot;

    @Column(nullable = false)
    private boolean hasSyrupHolder = true;

    @Column(nullable = false)
    private LocalDateTime lastRefilledAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "dispenser", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfMonth ASC")
    @ToString.Exclude
    private List<DispenserCompartment> compartments = new ArrayList<>();

    public Dispenser(Robot robot) {
        this.robot = robot;
        this.hasSyrupHolder = true;
        this.lastRefilledAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.lastRefilledAt == null) {
            this.lastRefilledAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---------- Relationship helpers (important for consistency) ----------

    public void addCompartment(DispenserCompartment compartment) {
        if (compartment == null) return;
        compartments.add(compartment);
        compartment.setDispenser(this);
    }

    public void removeCompartment(DispenserCompartment compartment) {
        if (compartment == null) return;
        compartments.remove(compartment);
        compartment.setDispenser(null);
    }

    /**
     * Convenience method: replace compartments safely.
     */
    public void setCompartments(List<DispenserCompartment> newCompartments) {
        this.compartments.clear();
        if (newCompartments != null) {
            newCompartments.forEach(this::addCompartment);
        }
    }
}
