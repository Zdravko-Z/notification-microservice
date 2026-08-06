package com.advanced.notification_microservice.scheduler;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.repository.NotificationRepository;
import com.advanced.notification_microservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReminderScheduler {
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(
            initialDelayString = "${notification.reminder.initial-delay-ms:30000}",
            fixedDelayString = "${notification.reminder.fixed-delay-ms:900000}"
    )
    public void sendUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now();
        long hoursBeforeAppointment = 24;
        LocalDateTime cutoff = now.plusHours(hoursBeforeAppointment);

        List<Notification> candidates = notificationRepository
                .findByTypeAndStatusAndReminderSentFalseAndCancelledFalseAndAppointmentTimeBetween(
                        NotificationType.CONFIRMATION, NotificationStatus.SENT, now, cutoff);

        if (candidates.isEmpty()) {
            return;
        }

        log.info("Reminder job: found {} booking(s) within {}h of their appointment", candidates.size(), hoursBeforeAppointment);

        for (Notification confirmation : candidates) {
            try {
                notificationService.sendReminderForBooking(confirmation.getId());
            } catch (Exception e) {
                log.error("Reminder job failed unexpectedly for booking {}: {}",
                        confirmation.getBookingId(), e.getMessage());
            }
        }
    }
}
