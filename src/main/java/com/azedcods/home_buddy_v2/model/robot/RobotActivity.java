package com.azedcods.home_buddy_v2.model.robot;

import com.azedcods.home_buddy_v2.model.enums.ActivitySeverity;
import com.azedcods.home_buddy_v2.model.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "robot_activity", indexes = {
        @Index(name = "idx_robot_activity_time", columnList = "activity_time")
})
@Getter
@Setter
@NoArgsConstructor
@ToString
public class RobotActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Robot robot;

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


}
