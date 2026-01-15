// src/main/java/com/azedcods/home_buddy_v2/repository/NotificationRepository.java
package com.azedcods.home_buddy_v2.repository.notif;

import com.azedcods.home_buddy_v2.model.notif.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    long countByRecipientUser_UserId(Long userId);

    @Query("""
        select count(n)
        from Notification n
        where n.recipientUser.userId = :userId
          and n.readAt is null
    """)
    long countUnread(@Param("userId") Long userId);

    // ✅ NEW: count unread across ALL users (admin all-mode)
    @Query("""
        select count(n)
        from Notification n
        where n.readAt is null
    """)
    long countUnreadAll();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.readAt = :now
        where n.recipientUser.userId = :userId
          and n.readAt is null
    """)
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    List<Notification> findByRecipientUser_UserIdOrderByCreatedAtAsc(Long userId, Pageable pageable);

    Optional<Notification> findTopByRecipientUser_UserIdAndNotificationKeyOrderByCreatedAtDesc(Long userId, String notificationKey);

    @Query("""
        select n
        from Notification n
        where n.recipientUser.userId = :userId
          and n.notificationKey = :key
          and n.createdAt >= :since
        order by n.createdAt desc
    """)
    Optional<Notification> findRecentByKey(@Param("userId") Long userId,
                                           @Param("key") String key,
                                           @Param("since") Instant since);

    Optional<Notification> findTopByRecipientUser_UserIdAndNotificationKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long userId, String notificationKey, Instant since
    );

}
