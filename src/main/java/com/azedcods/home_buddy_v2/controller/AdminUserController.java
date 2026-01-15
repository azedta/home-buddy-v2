package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.enums.AppRole;
import com.azedcods.home_buddy_v2.payload.UserPickDto;
import com.azedcods.home_buddy_v2.repository.auth.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ✅ for admin/caregiver dropdown
    @GetMapping("/pick")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CAREGIVER')")
    public List<UserPickDto> pickUsers() {

        Set<AppRole> blocked = Set.of(AppRole.ROLE_ADMIN, AppRole.ROLE_CAREGIVER);

        return userRepository.findAllPureUsers(AppRole.ROLE_USER, blocked).stream()
                .map(u -> new UserPickDto(
                        u.getUserId(),
                        u.getUserName(),
                        u.getFullname(),
                        u.getEmail()
                ))
                .toList();
    }

}
