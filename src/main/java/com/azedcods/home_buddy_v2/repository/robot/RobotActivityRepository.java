package com.azedcods.home_buddy_v2.repository.robot;

import com.azedcods.home_buddy_v2.enums.ActivitySeverity;
import com.azedcods.home_buddy_v2.enums.ActivityType;
import com.azedcods.home_buddy_v2.model.robot.RobotActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RobotActivityRepository extends JpaRepository<RobotActivity, Long> {

    long countByRobot_Id(String robotId);

    // used by trimToMax()
    List<RobotActivity> findByRobot_IdOrderByActivityTimeAsc(String robotId, Pageable pageable);

    // ✅ MISSING METHOD (your controller calls this)
    List<RobotActivity> findByRobot_IdOrderByActivityTimeDesc(String robotId, Pageable pageable);

    // ✅ STEP 6 anti-spam query
    @Query("""
            select count(a)
            from RobotActivity a
            where a.robot.id = :robotId
              and a.activityType = :type
              and a.severity in :severities
              and a.activityTime >= :since
            """)
    long countRecentByTypeAndSeverities(
            @Param("robotId") String robotId,
            @Param("type") ActivityType type,
            @Param("severities") List<ActivitySeverity> severities,
            @Param("since") Instant since
    );
}
