package com.advanced.notification_microservice.service;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.excption.NotificationNotFoundException;
import com.advanced.notification_microservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AsyncNotifcationServiceTest {
    @Mock
    private EmailService emailService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AsyncNotificationService asyncNotificationService;

    private Notification notification() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setBookingId(UUID.randomUUID());
        notification.setRecipientEmail("jane@example.com");
        notification.setSubject("Subject");
        notification.setBody("<p>body</p>");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);
        return notification;
    }

    @Test
    void attemptSend_whenEmailSucceeds_marksSentAndStampsSentAt() {
        Notification notification = notification();

        asyncNotificationService.attemptSend(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        verify(emailService).sendEmail(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
    }

    @Test
    void attemptSend_whenEmailThrows_marksFailedAndIncrementsRetryCount() {
        Notification notification = notification();
        notification.setRetryCount(1);
        doThrow(new RuntimeException("SMTP down"))
                .when(emailService).sendEmail(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());

        asyncNotificationService.attemptSend(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getRetryCount()).isEqualTo(2);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    void sendEmailAsync_whenFound_delegatesToAttemptSend() {
        Notification notification = notification();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        asyncNotificationService.sendEmailAsync(notification.getId());

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(emailService).sendEmail(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
    }

    @Test
    void sendEmailAsync_whenNotFound_throwsNotificationNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(notificationRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asyncNotificationService.sendEmailAsync(missingId))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
