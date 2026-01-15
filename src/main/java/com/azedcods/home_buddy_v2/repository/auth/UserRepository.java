package com.azedcods.home_buddy_v2.repository.auth;

import com.azedcods.home_buddy_v2.enums.AppRole;
import com.azedcods.home_buddy_v2.model.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String username);

    Boolean existsByUserName(String username);

    Boolean existsByEmail(String email);

    @Query("""
    select distinct u
    from User u
    join u.roles rUser
    where rUser.roleName = :userRole
      and not exists (
          select 1
          from User u2
          join u2.roles r2
          where u2 = u
            and r2.roleName in :blockedRoles
      )
    order by u.userName asc
""")
    List<User> findAllPureUsers(
            @Param("userRole") AppRole userRole,
            @Param("blockedRoles") Set<AppRole> blockedRoles
    );
}
