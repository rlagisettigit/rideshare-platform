package com.rideshare.platform.notification.service;

import com.rideshare.platform.notification.entity.Notification;
import com.rideshare.platform.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR: Section 12 Notification. Persists + dispatches across Push / SMS / Email / In-App channels.
 * Concrete channel senders (FCM, Twilio, SES, etc.) are external-system integration points -
 * wire real providers behind this service without touching event listeners.
 */
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationRepository notificationRepository;

    public void send(Long userId, String channel, String eventType, String title, String body) {
        send(userId, channel, eventType, title, body, null);
    }

    /** referenceId correlates this notification to a thing (e.g. a ride's public id) so a
     *  later event can find and remove it once it's superseded - see removeByReference(). */
    @Async
    public void send(Long userId, String channel, String eventType, String title, String body, String referenceId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setChannel(channel);
        n.setEventType(eventType);
        n.setReferenceId(referenceId);
        n.setTitle(title);
        n.setBody(body);
        n.setStatus("SENT"); // TODO: integrate real Push/SMS/Email provider, set FAILED on error
        notificationRepository.save(n);
    }

    // Derived delete queries need an active transaction to actually execute the underlying
    // EntityManager.remove() calls (unlike save(), which already gets one from
    // SimpleJpaRepository's own @Transactional) - hence the explicit annotation here.
    @Async
    @Transactional
    public void removeByReference(Long userId, String referenceId, String eventType) {
        notificationRepository.deleteByUserIdAndReferenceIdAndEventType(userId, referenceId, eventType);
    }
}
