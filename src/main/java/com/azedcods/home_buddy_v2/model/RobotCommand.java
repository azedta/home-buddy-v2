package com.azedcods.home_buddy_v2.model;


import com.azedcods.home_buddy_v2.model.enums.CommandStatus;
import com.azedcods.home_buddy_v2.model.enums.CommandType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "robot_command", indexes = {
        @Index(name = "idx_robot_command_time", columnList = "command_time")
})
@Getter
@Setter
@NoArgsConstructor
@ToString
public class RobotCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Robot robot;

    @Column(name = "activity_time", nullable = false)
    private Instant activityTime;

    @Column(name = "command_time", nullable = false)
    private Instant commandTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommandType commandType;

    @Column(length = 120)
    private String targetLocation;

    @Column(nullable = false, length = 300)
    private String description; // logged string you wanted

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CommandStatus status;

}
