package com.advanced.notification_microservice.dto;

import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private String recipientEmail;
    private String recipientPhone;
    private String subject;
    private NotificationStatus status;
    private NotificationType type;
    private int retryCount;
    private Instant sentAt;
    private LocalDateTime appointmentTime;
    private boolean reminderSent;
    private boolean cancelled;
}
