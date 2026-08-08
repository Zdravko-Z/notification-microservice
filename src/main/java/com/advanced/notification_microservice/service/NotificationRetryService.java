package com.advanced.notification_microservice.service;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.excption.NotificationNotFoundException;
import com.advanced.notification_microservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRetryService {
    private final NotificationRepository notificationRepository;
    private final AsyncNotificationService asyncNotificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("There is no notification with this id: " + notificationId));

        log.info("Retrying notification {} for booking {} (attempt {})",
                notification.getId(), notification.getBookingId(), notification.getRetryCount() + 1);

        asyncNotificationService.attemptSend(notification);
    }
}
