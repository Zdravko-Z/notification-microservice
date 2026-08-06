package com.advanced.notification_microservice.scheduler;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.repository.NotificationRepository;
import com.advanced.notification_microservice.service.NotificationRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {
    private final NotificationRepository notificationRepository;
    private final NotificationRetryService notificationRetryService;

    @Scheduled(
            initialDelayString = "${notification.retry.initial-delay-ms:30000}",
            fixedDelayString = "${notification.retry.fixed-delay-ms:60000}"
    )
    public void retryFailedNotifications() {
        int maxAttempts = 3;
        List<Notification> candidates =
                notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, maxAttempts);

        if (candidates.isEmpty()) {
            return;
        }

        log.info("Notification retry job: found {} failed notification(s) eligible for retry", candidates.size());

        for (Notification notification : candidates) {
            try {
                notificationRetryService.retryNotification(notification.getId());
            } catch (Exception e) {
                log.error("Retry job failed unexpectedly for notification {}: {}",
                        notification.getId(), e.getMessage());
            }
        }
    }
}
