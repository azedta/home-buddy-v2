package com.azedcods.home_buddy_v2.payload;

import com.azedcods.home_buddy_v2.model.enums.*;

import java.time.Instant;


public class RobotDtos {

    public record StatusResponse(
            String id,
            Integer batteryLevel,
            RobotStatus robotStatus,
            TrayStatus trayStatus,
            ToggleState sensorStatus,
            String sensorMessage,
            ToggleState dispenserStatus,
            FillLevel dispenserFillLevel,
            Integer dispenserPillsRemaining,
            String currentLocation,
            String targetLocation,
            Instant lastUpdatedAt
    ) {}

    public record ActivityResponse(
            Long id,
            Instant activityTime,
            ActivityType activityType,
            ActivitySeverity severity,
            String activityDescription
    ) {}

    public record CommandRequest(
            CommandType commandType,
            String targetLocation,
            String description
    ) {}

    public record CommandResponse(
            Long id,
            Instant commandTime,
            CommandType commandType,
            String targetLocation,
            String description,
            CommandStatus status
    ) {}
}