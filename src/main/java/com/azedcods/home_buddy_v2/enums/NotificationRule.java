package com.azedcods.home_buddy_v2.enums;

public enum NotificationRule {

    // --- medication / doses ---
    DOSE_DUE,
    DOSE_CONFIRM_REQUIRED,
    DOSE_MISSED,
    DOSE_TAKEN,
    DOSE_TAKEN_LATE,
    DOSE_DUPLICATE_ATTEMPT,

    // --- dispenser ---
    DISPENSER_LOW,
    DISPENSER_EMPTY,
    DISPENSE_FAILED,
    DISPENSE_SUCCESS,

    // --- robot ---
    ROBOT_DOWN,
    ROBOT_RECOVERED,
    ROBOT_BATTERY_LOW,
    ROBOT_BATTERY_CRITICAL,
    ROBOT_STUCK,
    ROBOT_SENSOR_DISABLED,

    // --- security/system ---
    LOGIN_NEW_DEVICE,
    LOGIN_FAILED_ATTEMPTS,
    PERMISSION_DENIED,
    SYSTEM_INTEGRITY_WARNING,
    SYSTEM_INTEGRITY_CRITICAL
}
