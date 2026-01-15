package com.azedcods.home_buddy_v2.payload;

import com.azedcods.home_buddy_v2.model.auth.UserSettings;

import java.time.Instant;

public class SettingsDtos {

    public record SettingsResponse(
            Long userId,
            String displayName,
            String avatarColor,
            String timeZone,
            String locale,
            UserSettings.ThemeMode themeMode,
            boolean notifyEmail,
            boolean notifyPush,
            boolean doseReminders,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record UpdateSettingsRequest(
            String displayName,
            String avatarColor,
            String timeZone,
            String locale,
            UserSettings.ThemeMode themeMode,
            Boolean notifyEmail,
            Boolean notifyPush,
            Boolean doseReminders
    ) {}
}
