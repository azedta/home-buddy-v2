package com.azedcods.home_buddy_v2.model.robot;

import com.azedcods.home_buddy_v2.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name="robot")
public class Robot {

    public static final String ROBOT_ID = "LEO-4523124-VAC";

    @Id
    private String id = ROBOT_ID;


    @Column(nullable = false)
    private String robotName = "HomeBuddy-v1";

    @Column(nullable = false)
    private Integer batteryLevel; // 0..100

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RobotStatus robotStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrayStatus trayStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ToggleState sensorStatus; // ON/OFF

    @Column(length = 255)
    private String sensorMessage; // "Detected obstacle at 0.8m" etc

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ToggleState dispenserStatus; // ON/OFF

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FillLevel dispenserFillLevel; // FULL/EMPTY

    @Column(nullable = false)
    private Integer dispenserPillsRemaining; // e.g. 0..30

    @Column(nullable = false, length = 80)
    private String currentLocation; // "LIVING_ROOM", "KITCHEN"

    @Column(nullable = false, length = 80)
    private String targetLocation; // where it’s going next

    @Column(nullable = false)
    private Instant lastUpdatedAt;

    @Version
    private Long version;


}

