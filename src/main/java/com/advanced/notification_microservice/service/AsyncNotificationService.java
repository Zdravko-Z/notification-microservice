package com.advanced.notification_microservice.service;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.excption.NotificationNotFoundException;
import com.advanced.notification_microservice.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncNotificationService {
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendEmailAsync(UUID notificationId){
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("There is no notification with this id: " + notificationId));

        try {
            emailService.sendEmail(notification.getRecipientEmail(),
                    notification.getSubject(),
                    notification.getBody());

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());

            log.info("Email sent successfully for booking: {}", notification.getBookingId());

        }catch (MessagingException | UnsupportedEncodingException e){
            log.error("Failed to send email for booking {}: {}", notification.getBookingId(), e.getMessage());

            notification.setStatus(NotificationStatus.FAILED);
            notification.setRetryCount(notification.getRetryCount() + 1);
        }
    }
}
