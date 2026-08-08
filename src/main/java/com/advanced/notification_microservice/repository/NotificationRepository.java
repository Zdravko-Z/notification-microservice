package com.advanced.notification_microservice.repository;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.entity.NotificationType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserId(UUID userId, Sort sort);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int retryCount);

    List<Notification> findByTypeAndStatusAndReminderSentFalseAndCancelledFalseAndAppointmentTimeBetween(
            NotificationType type, NotificationStatus status, LocalDateTime from, LocalDateTime to);

    List<Notification> findByBookingId(UUID bookingId);}
