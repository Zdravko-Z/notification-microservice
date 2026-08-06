package com.advanced.notification_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {
    private UUID userId;
    private boolean confirmationEnabled;
    private boolean reminderEnabled;
    private boolean marketingEnabled;
    private boolean unsubscribedAll;
    private Instant updatedAt;
}
