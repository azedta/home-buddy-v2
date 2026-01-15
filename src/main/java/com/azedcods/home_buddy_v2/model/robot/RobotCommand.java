package com.azedcods.home_buddy_v2.model.robot;


import com.azedcods.home_buddy_v2.enums.CommandStatus;
import com.azedcods.home_buddy_v2.enums.CommandType;
import com.azedcods.home_buddy_v2.enums.HouseLocation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name="robot_command",
        indexes = {
                @Index(name="idx_command_robot_time", columnList="robot_id, command_time")
        }
)

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RobotCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_time", nullable = false)
    private Instant commandTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommandType commandType;

    @Column(length = 120)
    private HouseLocation targetLocation;

    @Column(nullable = false, length = 300)
    private String description; // logged string you wanted

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CommandStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;


}
