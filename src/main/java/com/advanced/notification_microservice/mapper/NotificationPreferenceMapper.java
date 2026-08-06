package com.advanced.notification_microservice.mapper;

import com.advanced.notification_microservice.dto.NotificationPreferenceResponse;
import com.advanced.notification_microservice.entity.NotificationPreference;

public class NotificationPreferenceMapper {
    public static NotificationPreferenceResponse toResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .userId(preference.getUserId())
                .confirmationEnabled(preference.isConfirmationEnabled())
                .reminderEnabled(preference.isReminderEnabled())
                .marketingEnabled(preference.isMarketingEnabled())
                .unsubscribedAll(preference.isUnsubscribedAll())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
