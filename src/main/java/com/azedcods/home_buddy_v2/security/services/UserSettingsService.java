package com.azedcods.home_buddy_v2.security.services;

import com.azedcods.home_buddy_v2.model.auth.UserSettings;
import com.azedcods.home_buddy_v2.payload.SettingsDtos;
import com.azedcods.home_buddy_v2.repository.auth.UserSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserSettingsService {

    private final UserSettingsRepository repo;

    public UserSettingsService(UserSettingsRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public SettingsDtos.SettingsResponse getOrCreate(Long userId) {
        UserSettings s = repo.findByUserId(userId).orElseGet(() -> {
            UserSettings created = new UserSettings();
            created.setUserId(userId);
            created.setThemeMode(UserSettings.ThemeMode.SYSTEM);
            created.setNotifyEmail(true);
            created.setNotifyPush(true);
            created.setDoseReminders(true);
            created.setLocale(defaultLocale());
            created.setTimeZone(defaultTimeZone());
            return repo.save(created);
        });

        return toResponse(s);
    }

    @Transactional
    public SettingsDtos.SettingsResponse update(Long userId, SettingsDtos.UpdateSettingsRequest req) {
        UserSettings s = repo.findByUserId(userId).orElseGet(() -> {
            UserSettings created = new UserSettings();
            created.setUserId(userId);
            return created;
        });

        if (req.displayName() != null) s.setDisplayName(trimToNull(req.displayName(), 80));
        if (req.avatarColor() != null) s.setAvatarColor(trimToNull(req.avatarColor(), 30));

        if (req.timeZone() != null) s.setTimeZone(trimToNull(req.timeZone(), 60));
        if (req.locale() != null) s.setLocale(trimToNull(req.locale(), 20));
        if (req.themeMode() != null) s.setThemeMode(req.themeMode());

        if (req.notifyEmail() != null) s.setNotifyEmail(req.notifyEmail());
        if (req.notifyPush() != null) s.setNotifyPush(req.notifyPush());
        if (req.doseReminders() != null) s.setDoseReminders(req.doseReminders());

        UserSettings saved = repo.save(s);
        return toResponse(saved);
    }

    private SettingsDtos.SettingsResponse toResponse(UserSettings s) {
        return new SettingsDtos.SettingsResponse(
                s.getUserId(),
                s.getDisplayName(),
                s.getAvatarColor(),
                s.getTimeZone(),
                s.getLocale(),
                s.getThemeMode(),
                s.isNotifyEmail(),
                s.isNotifyPush(),
                s.isDoseReminders(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    private String defaultTimeZone() {
        // You can change default to match your app.
        return "America/Toronto";
    }

    private String defaultLocale() {
        return Locale.getDefault().toLanguageTag(); // e.g. "en-CA"
    }

    private String trimToNull(String s, int maxLen) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > maxLen) t = t.substring(0, maxLen);
        return t;
    }
}
