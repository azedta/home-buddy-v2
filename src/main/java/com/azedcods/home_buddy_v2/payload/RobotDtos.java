package com.azedcods.home_buddy_v2.payload;

import com.azedcods.home_buddy_v2.enums.*;

import java.time.Instant;

public class RobotDtos {

    public record StatusResponse(
            String id,
            String robotName,
            Long assistedUserId,
            Integer batteryLevel,
            RobotStatus robotStatus,
            TrayStatus trayStatus,
            ToggleState sensorStatus,
            String sensorMessage,
            ToggleState dispenserStatus,
            FillLevel dispenserFillLevel,
            Integer dispenserPillsRemaining,
            HouseLocation currentLocation,
            HouseLocation targetLocation,
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
            HouseLocation targetLocation,
            String description,
            CommandStatus status
    ) {}
}
