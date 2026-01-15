package com.azedcods.home_buddy_v2.model.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(
        name = "user_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_settings_user", columnNames = {"user_id"})
)
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We intentionally do NOT FK to a specific users table name (keeps this working across schemas).
    // If you want a real FK later, add it in SQL once your user table name is confirmed.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // --- profile-ish ---
    @Column(name = "display_name", length = 80)
    private String displayName;

    @Column(name = "avatar_color", length = 30)
    private String avatarColor; // e.g. "slate", "blue", "emerald" (optional)

    // --- preferences ---
    @Column(name = "time_zone", length = 60)
    private String timeZone; // e.g. "America/Toronto"

    @Column(name = "locale", length = 20)
    private String locale; // e.g. "en-CA"

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_mode", nullable = false, length = 12)
    private ThemeMode themeMode = ThemeMode.SYSTEM;

    // --- notifications ---
    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail = true;

    @Column(name = "notify_push", nullable = false)
    private boolean notifyPush = true;

    @Column(name = "dose_reminders", nullable = false)
    private boolean doseReminders = true;

    // --- audit ---
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (themeMode == null) themeMode = ThemeMode.SYSTEM;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (themeMode == null) themeMode = ThemeMode.SYSTEM;
    }

    public enum ThemeMode {
        SYSTEM, LIGHT, DARK
    }
}

