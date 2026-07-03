package com.rideshare.platform.notification.repository;

import com.rideshare.platform.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Removes a now-superseded notification, e.g. "ride started" once the ride is completed. */
    void deleteByUserIdAndReferenceIdAndEventType(Long userId, String referenceId, String eventType);
}
