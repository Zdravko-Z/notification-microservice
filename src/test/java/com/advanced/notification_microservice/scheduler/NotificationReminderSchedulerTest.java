package com.advanced.notification_microservice.scheduler;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.repository.NotificationRepository;
import com.advanced.notification_microservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationReminderSchedulerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationReminderScheduler scheduler;

    private Notification confirmationDueForReminder() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setBookingId(UUID.randomUUID());
        notification.setType(NotificationType.CONFIRMATION);
        notification.setStatus(NotificationStatus.SENT);
        notification.setAppointmentTime(LocalDateTime.now().plusHours(20));
        return notification;
    }

    @Test
    void sendUpcomingReminders_noCandidates_neverCallsService() {
        when(notificationRepository.findByTypeAndStatusAndReminderSentFalseAndCancelledFalseAndAppointmentTimeBetween(
                eq(NotificationType.CONFIRMATION), eq(NotificationStatus.SENT), any(), any()))
                .thenReturn(List.of());

        scheduler.sendUpcomingReminders();

        verify(notificationService, never()).sendReminderForBooking(any());
    }

    @Test
    void sendUpcomingReminders_withCandidates_sendsForEachBooking() {
        Notification first = confirmationDueForReminder();
        Notification second = confirmationDueForReminder();
        when(notificationRepository.findByTypeAndStatusAndReminderSentFalseAndCancelledFalseAndAppointmentTimeBetween(
                eq(NotificationType.CONFIRMATION), eq(NotificationStatus.SENT), any(), any()))
                .thenReturn(List.of(first, second));

        scheduler.sendUpcomingReminders();

        verify(notificationService).sendReminderForBooking(eq(first.getId()));
        verify(notificationService).sendReminderForBooking(eq(second.getId()));
    }

    @Test
    void sendUpcomingReminders_oneBookingThrows_stillProcessesTheRest() {
        Notification first = confirmationDueForReminder();
        Notification second = confirmationDueForReminder();
        when(notificationRepository.findByTypeAndStatusAndReminderSentFalseAndCancelledFalseAndAppointmentTimeBetween(
                eq(NotificationType.CONFIRMATION), eq(NotificationStatus.SENT), any(), any()))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("boom")).when(notificationService).sendReminderForBooking(first.getId());

        scheduler.sendUpcomingReminders();

        verify(notificationService).sendReminderForBooking(eq(second.getId()));
    }
}

