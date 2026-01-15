package com.azedcods.home_buddy_v2.repository.dose;

import com.azedcods.home_buddy_v2.model.dose.Dose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DoseRepository extends JpaRepository<Dose, Long> {

    List<Dose> findByUser_UserId(Long userId);

    @Query("select distinct d.user.userId from Dose d where d.user is not null")
    List<Long> findDistinctUserIdsWithDoses();
}
