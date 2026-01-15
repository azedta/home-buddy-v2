package com.azedcods.home_buddy_v2.repository.auth;

import com.azedcods.home_buddy_v2.model.auth.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUserId(Long userId);
}

