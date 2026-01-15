package com.azedcods.home_buddy_v2.payload;

public record UserPickDto(
        Long id,
        String username,
        String fullname,
        String email
) {}