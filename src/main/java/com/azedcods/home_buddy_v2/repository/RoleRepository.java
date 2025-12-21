package com.azedcods.home_buddy_v2.repository;


import com.azedcods.home_buddy_v2.model.AppRole;
import com.azedcods.home_buddy_v2.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(AppRole appRole);
}
