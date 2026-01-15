package com.azedcods.home_buddy_v2.model.robot;

import com.azedcods.home_buddy_v2.enums.ActivitySeverity;
import com.azedcods.home_buddy_v2.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name="robot_activity",
        indexes = {
                @Index(name="idx_activity_robot_time", columnList="robot_id, activity_time")
        }
)

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RobotActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_time", nullable = false)
    private Instant activityTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivitySeverity severity;

    @Column(nullable = false, length = 500)
    private String activityDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;



}
