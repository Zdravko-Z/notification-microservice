package com.advanced.notification_microservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_notification_type",
                columnNames = {"booking_id", "notification_type"}))
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id",nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "treatment_name")
    private String treatmentName;

    @Column(name = "confirmation_code")
    private String confirmationCode;

    @Column(name = "appointment_time")
    private LocalDateTime appointmentTime;

    @Column
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    private NotificationType type;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent = false;

    @Column(name = "cancelled", nullable = false)
    private boolean cancelled = false;
}
