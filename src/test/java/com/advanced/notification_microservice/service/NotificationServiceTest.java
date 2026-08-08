package com.advanced.notification_microservice.service;

import com.advanced.notification_microservice.dto.NotificationRequest;
import com.advanced.notification_microservice.dto.NotificationResponse;
import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.excption.NotificationNotFoundException;
import com.advanced.notification_microservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AsyncNotificationService asyncNotificationService;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationRequest request;

    @BeforeEach
    void setUp() {
        request = NotificationRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .email("jane@example.com")
                .phone("+1-555-0100")
                .confirmationCode("ABC123")
                .customerName("Jane Doe")
                .appointmentTime(LocalDateTime.now().plusDays(2))
                .treatmentName("Deep Tissue Massage")
                .build();

        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendConfirmation_whenAllowed_savesAndDispatchesEmail() {
        when(notificationPreferenceService.isAllowed(request.getUserId(), NotificationType.CONFIRMATION))
                .thenReturn(true);

        notificationService.sendConfirmation(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getBookingId()).isEqualTo(request.getBookingId());
        assertThat(saved.getType()).isEqualTo(NotificationType.CONFIRMATION);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(asyncNotificationService).sendEmailAsync(any());
    }

    @Test
    void sendConfirmation_whenBlockedByPreference_marksSuppressedAndNeverDispatches() {
        when(notificationPreferenceService.isAllowed(request.getUserId(), NotificationType.CONFIRMATION))
                .thenReturn(false);

        notificationService.sendConfirmation(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SUPPRESSED);
        verify(asyncNotificationService, never()).sendEmailAsync(any());
    }

    private Notification confirmationFixture() {
        Notification confirmation = new Notification();
        confirmation.setId(UUID.randomUUID());
        confirmation.setBookingId(UUID.randomUUID());
        confirmation.setUserId(UUID.randomUUID());
        confirmation.setRecipientEmail("jane@example.com");
        confirmation.setRecipientPhone("+1-555-0100");
        confirmation.setCustomerName("Jane Doe");
        confirmation.setTreatmentName("Hot Stone Therapy");
        confirmation.setConfirmationCode("XYZ789");
        confirmation.setAppointmentTime(LocalDateTime.now().plusHours(20));
        confirmation.setType(NotificationType.CONFIRMATION);
        confirmation.setStatus(NotificationStatus.SENT);
        confirmation.setReminderSent(false);
        return confirmation;
    }

    @Test
    void sendReminderForBooking_whenAlreadyReminded_doesNothing() {
        Notification confirmation = confirmationFixture();
        confirmation.setReminderSent(true);
        when(notificationRepository.findById(confirmation.getId())).thenReturn(Optional.of(confirmation));

        notificationService.sendReminderForBooking(confirmation.getId());

        verify(notificationRepository, never()).save(any());
        verify(asyncNotificationService, never()).sendEmailAsync(any());
    }

    @Test
    void sendReminderForBooking_whenConfirmationMissing_throwsNotFound() {
        UUID missingId = UUID.randomUUID();
        when(notificationRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.sendReminderForBooking(missingId))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void sendReminderForBooking_whenAllowed_savesReminderDispatchesAndMarksReminderSent() {
        Notification confirmation = confirmationFixture();
        when(notificationRepository.findById(confirmation.getId())).thenReturn(Optional.of(confirmation));
        when(notificationPreferenceService.isAllowed(confirmation.getUserId(), NotificationType.REMINDER))
                .thenReturn(true);

        notificationService.sendReminderForBooking(confirmation.getId());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification reminder = captor.getValue();

        assertThat(reminder.getType()).isEqualTo(NotificationType.REMINDER);
        assertThat(reminder.getBookingId()).isEqualTo(confirmation.getBookingId());
        assertThat(reminder.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(confirmation.isReminderSent()).isTrue();
        verify(asyncNotificationService).sendEmailAsync(any());
    }

    @Test
    void sendReminderForBooking_whenBlockedByPreference_marksSuppressedButStillMarksReminderSent() {
        Notification confirmation = confirmationFixture();
        when(notificationRepository.findById(confirmation.getId())).thenReturn(Optional.of(confirmation));
        when(notificationPreferenceService.isAllowed(confirmation.getUserId(), NotificationType.REMINDER))
                .thenReturn(false);

        notificationService.sendReminderForBooking(confirmation.getId());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SUPPRESSED);
        assertThat(confirmation.isReminderSent()).isTrue();
        verify(asyncNotificationService, never()).sendEmailAsync(any());
    }

    @Test
    void sendReminderForBooking_whenDuplicateReminderRow_stillMarksReminderSentAndDoesNotThrow() {
        Notification confirmation = confirmationFixture();
        when(notificationRepository.findById(confirmation.getId())).thenReturn(Optional.of(confirmation));
        when(notificationPreferenceService.isAllowed(confirmation.getUserId(), NotificationType.REMINDER))
                .thenReturn(true);
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate booking_id/notification_type"));

        notificationService.sendReminderForBooking(confirmation.getId());

        assertThat(confirmation.isReminderSent()).isTrue();
        verify(asyncNotificationService, never()).sendEmailAsync(any());
    }

    @Test
    void cancelNotificationsForBooking_pendingAndFailedBecomeCancelled_sentStaysAsIs() {
        UUID bookingId = UUID.randomUUID();

        Notification pending = new Notification();
        pending.setStatus(NotificationStatus.PENDING);

        Notification failed = new Notification();
        failed.setStatus(NotificationStatus.FAILED);

        Notification sent = new Notification();
        sent.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByBookingId(bookingId)).thenReturn(List.of(pending, failed, sent));

        notificationService.cancelNotificationsForBooking(bookingId);

        assertThat(pending.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        assertThat(pending.isCancelled()).isTrue();

        assertThat(failed.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        assertThat(failed.isCancelled()).isTrue();

        assertThat(sent.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(sent.isCancelled()).isTrue();
    }

    @Test
    void cancelNotificationsForBooking_noneFound_throwsNotFound() {
        UUID bookingId = UUID.randomUUID();
        when(notificationRepository.findByBookingId(bookingId)).thenReturn(List.of());

        assertThatThrownBy(() -> notificationService.cancelNotificationsForBooking(bookingId))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void getHistory_withUserId_queriesByUser() {
        UUID userId = UUID.randomUUID();
        Notification notification = confirmationFixture();
        notification.setUserId(userId);
        when(notificationRepository.findByUserId(eq(userId), any())).thenReturn(List.of(notification));

        List<NotificationResponse> history = notificationService.getHistory(userId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getUserId()).isEqualTo(userId);
        verify(notificationRepository).findByUserId(eq(userId), any());
        verify(notificationRepository, never()).findAll(any(org.springframework.data.domain.Sort.class));
    }

    @Test
    void getHistory_withoutUserId_queriesAll() {
        Notification notification = confirmationFixture();
        when(notificationRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(notification));

        List<NotificationResponse> history = notificationService.getHistory(null);

        assertThat(history).hasSize(1);
        verify(notificationRepository).findAll(any(org.springframework.data.domain.Sort.class));
        verify(notificationRepository, never()).findByUserId(any(), any());
    }
}
