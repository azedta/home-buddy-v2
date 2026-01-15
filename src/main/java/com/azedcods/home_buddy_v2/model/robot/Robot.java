package com.azedcods.home_buddy_v2.model.robot;

import com.azedcods.home_buddy_v2.enums.*;
import com.azedcods.home_buddy_v2.model.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(
        name="robot",
        uniqueConstraints = @UniqueConstraint(
                name="uk_robot_assisted_user",
                columnNames="assisted_user_id"
        ),
        indexes = {
                @Index(name="idx_robot_assisted_user", columnList="assisted_user_id")
        }
)

public class Robot {

    @Id
    @Column(length = 32)
    private String id;   // generated in service/bootstrap

    @Column(nullable = false)
    private String robotName = "HomeBuddy-v2";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assisted_user_id")
    private User assistedUser;


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
    private HouseLocation currentLocation; // "LIVING_ROOM", "KITCHEN"

    @Column(nullable = false, length = 80)
    private HouseLocation targetLocation; // where it’s going next

    @Column(nullable = false)
    private Instant lastUpdatedAt;

    @Version
    private Long version;


}

