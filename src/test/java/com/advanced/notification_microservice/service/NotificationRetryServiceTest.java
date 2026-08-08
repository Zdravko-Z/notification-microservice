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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRetryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AsyncNotificationService asyncNotificationService;

    @InjectMocks
    private NotificationRetryService notificationRetryService;

    @Test
    void retryNotification_whenFound_delegatesToAttemptSend() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setBookingId(UUID.randomUUID());
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(1);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        notificationRetryService.retryNotification(notification.getId());

        verify(asyncNotificationService).attemptSend(notification);
    }

    @Test
    void retryNotification_whenNotFound_throwsNotificationNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(notificationRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationRetryService.retryNotification(missingId))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
