package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.payload.SettingsDtos;
import com.azedcods.home_buddy_v2.security.AuthUserId;
import com.azedcods.home_buddy_v2.security.services.UserSettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {

    private final UserSettingsService service;

    public UserSettingsController(UserSettingsService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public SettingsDtos.SettingsResponse me() {
        Long userId = AuthUserId.requireUserId();
        return service.getOrCreate(userId);
    }

    @PutMapping("/me")
    public SettingsDtos.SettingsResponse updateMe(@RequestBody SettingsDtos.UpdateSettingsRequest req) {
        Long userId = AuthUserId.requireUserId();
        return service.update(userId, req);
    }
}
