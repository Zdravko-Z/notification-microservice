package com.advanced.notification_microservice.scheduler;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.repository.NotificationRepository;
import com.advanced.notification_microservice.service.NotificationRetryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRetrySchedulerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationRetryService notificationRetryService;

    @InjectMocks
    private NotificationRetryScheduler scheduler;

    private Notification failedNotification() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setStatus(NotificationStatus.FAILED);
        return notification;
    }

    @Test
    void retryFailedNotifications_noCandidates_neverCallsRetryService() {
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .thenReturn(List.of());

        scheduler.retryFailedNotifications();

        verify(notificationRetryService, never()).retryNotification(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retryFailedNotifications_withCandidates_retriesEachOne() {
        Notification first = failedNotification();
        Notification second = failedNotification();
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .thenReturn(List.of(first, second));

        scheduler.retryFailedNotifications();

        verify(notificationRetryService).retryNotification(eq(first.getId()));
        verify(notificationRetryService).retryNotification(eq(second.getId()));
    }

    @Test
    void retryFailedNotifications_oneCandidateThrows_stillProcessesTheRest() {
        Notification first = failedNotification();
        Notification second = failedNotification();
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("boom")).when(notificationRetryService).retryNotification(first.getId());

        scheduler.retryFailedNotifications();

        verify(notificationRetryService).retryNotification(eq(second.getId()));
    }
}
