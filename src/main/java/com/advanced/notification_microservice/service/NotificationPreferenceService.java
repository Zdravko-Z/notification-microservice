package com.advanced.notification_microservice.service;

import com.advanced.notification_microservice.dto.NotificationPreferenceRequest;
import com.advanced.notification_microservice.entity.NotificationPreference;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationPreferenceService {
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreference getOrCreateDefault(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference preference = new NotificationPreference();
                    preference.setUserId(userId);
                    preference.setUpdatedAt(Instant.now());
                    return preferenceRepository.save(preference);
                });
    }

    public NotificationPreference findByUserIdOrDefault(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference preference = new NotificationPreference();
                    preference.setUserId(userId);
                    return preference;
                });
    }

    public NotificationPreference updatePreferences(UUID userId, NotificationPreferenceRequest request) {
        NotificationPreference preference = getOrCreateDefault(userId);

        preference.setConfirmationEnabled(request.getConfirmationEnabled());
        preference.setReminderEnabled(request.getReminderEnabled());
        preference.setMarketingEnabled(request.getMarketingEnabled());
        preference.setUpdatedAt(Instant.now());

        log.info("Updated notification preferences for user {}: confirmation={}, reminder={}, marketing={}",
                userId, preference.isConfirmationEnabled(), preference.isReminderEnabled(), preference.isMarketingEnabled());

        return preference;
    }

    public NotificationPreference unsubscribeAll(UUID userId) {
        NotificationPreference preference = getOrCreateDefault(userId);
        preference.setUnsubscribedAll(true);
        preference.setUpdatedAt(Instant.now());

        log.info("User {} unsubscribed from all notifications", userId);
        return preference;
    }

    public NotificationPreference resubscribeAll(UUID userId) {
        NotificationPreference preference = getOrCreateDefault(userId);
        preference.setUnsubscribedAll(false);
        preference.setUpdatedAt(Instant.now());

        log.info("User {} resubscribed to notifications", userId);
        return preference;
    }

    public boolean isAllowed(UUID userId, NotificationType type) {
        NotificationPreference preference = findByUserIdOrDefault(userId);

        if (preference.isUnsubscribedAll()) {
            return false;
        }

        return switch (type) {
            case CONFIRMATION -> preference.isConfirmationEnabled();
            case REMINDER -> preference.isReminderEnabled();
            case MARKETING -> preference.isMarketingEnabled();
        };
    }
}
