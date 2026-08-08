package com.advanced.notification_microservice.repository;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private Notification newNotification(UUID bookingId, UUID userId, NotificationType type, NotificationStatus status) {
        Notification notification = new Notification();
        notification.setBookingId(bookingId);
        notification.setUserId(userId);
        notification.setRecipientEmail("jane@example.com");
        notification.setSubject("Subject");
        notification.setBody("<p>body</p>");
        notification.setType(type);
        notification.setStatus(status);
        return notification;
    }

    @Test
    void uniqueConstraint_sameBookingAndSameType_secondInsertFails() {
        UUID bookingId = UUID.randomUUID();
        notificationRepository.saveAndFlush(
                newNotification(bookingId, UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.SENT));

        Notification duplicate = newNotification(bookingId, UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.PENDING);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueConstraint_sameBookingDifferentType_bothSaveSuccessfully() {
        UUID bookingId = UUID.randomUUID();
        notificationRepository.saveAndFlush(
                newNotification(bookingId, UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.SENT));
        notificationRepository.saveAndFlush(
                newNotification(bookingId, UUID.randomUUID(), NotificationType.REMINDER, NotificationStatus.PENDING));

        List<Notification> forBooking = notificationRepository.findByBookingId(bookingId);
        assertThat(forBooking).hasSize(2);
    }

    @Test
    void findByStatusAndRetryCountLessThan_onlyReturnsEligibleFailures() {
        Notification eligible = newNotification(UUID.randomUUID(), UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.FAILED);
        eligible.setRetryCount(1);

        Notification exhausted = newNotification(UUID.randomUUID(), UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.FAILED);
        exhausted.setRetryCount(3);

        Notification notFailed = newNotification(UUID.randomUUID(), UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.SENT);
        notFailed.setRetryCount(0);

        notificationRepository.saveAll(List.of(eligible, exhausted, notFailed));

        List<Notification> result = notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);

        assertThat(result).extracting(Notification::getId).containsExactly(eligible.getId());
    }

    @Test
    void findByTypeAndStatusAndReminderSentFalseAndCancelledFalseAndAppointmentTimeBetween_filtersCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        Notification dueSoon = newNotification(UUID.randomUUID(), UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.SENT);
        dueSoon.setAppointmentTime(now.plusHours(10));
        dueSoon.setReminderSent(false);
        dueSoon.setCancelled(false);

        Notification tooFarOut = newNotification(UUID.randomUUID(), UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.SENT);
        tooFarOut.setAppointmentTime(now.plusDays(5));
        tooFarOut.setReminderSent(false);
        tooFarOut.setCancelled(false);

        Notification alreadyReminded = newNotification(UUID.randomUUID(), UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.SENT);
        alreadyReminded.setAppointmentTime(now.plusHours(10));
        alreadyReminded.setReminderSent(true);
        alreadyReminded.setCancelled(false);

        Notification cancelledBooking = newNotification(UUID.randomUUID(), UUID.randomUUID(), NotificationType.CONFIRMATION, NotificationStatus.SENT);
        cancelledBooking.setAppointmentTime(now.plusHours(10));
        cancelledBooking.setReminderSent(false);
        cancelledBooking.setCancelled(true);

        notificationRepository.saveAll(List.of(dueSoon, tooFarOut, alreadyReminded, cancelledBooking));

        List<Notification> result = notificationRepository
                .findByTypeAndStatusAndReminderSentFalseAndCancelledFalseAndAppointmentTimeBetween(
                        NotificationType.CONFIRMATION, NotificationStatus.SENT, now, now.plusHours(24));

        assertThat(result).extracting(Notification::getId).containsExactly(dueSoon.getId());
    }

    @Test
    void findByUserId_withSort_ordersByRequestedDirection() {
        UUID userId = UUID.randomUUID();

        Notification older = newNotification(UUID.randomUUID(), userId, NotificationType.CONFIRMATION, NotificationStatus.SENT);
        older.setSentAt(Instant.now().minusSeconds(3600));

        Notification newer = newNotification(UUID.randomUUID(), userId, NotificationType.REMINDER, NotificationStatus.SENT);
        newer.setSentAt(Instant.now());

        notificationRepository.saveAll(List.of(older, newer));

        List<Notification> result = notificationRepository.findByUserId(userId, Sort.by(Sort.Direction.DESC, "sentAt"));

        assertThat(result).extracting(Notification::getId).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findByBookingId_returnsEmptyList_whenNoneExist() {
        List<Notification> result = notificationRepository.findByBookingId(UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}


