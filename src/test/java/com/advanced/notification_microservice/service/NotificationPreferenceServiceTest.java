package com.advanced.notification_microservice.service;

import com.advanced.notification_microservice.dto.NotificationPreferenceRequest;
import com.advanced.notification_microservice.entity.NotificationPreference;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    @Test
    void getOrCreateDefault_existingRow_returnsItWithoutSaving() {
        UUID userId = UUID.randomUUID();
        NotificationPreference existing = new NotificationPreference();
        existing.setUserId(userId);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        NotificationPreference result = preferenceService.getOrCreateDefault(userId);

        assertThat(result).isSameAs(existing);
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void getOrCreateDefault_missingRow_createsAndSavesDefault() {
        UUID userId = UUID.randomUUID();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = preferenceService.getOrCreateDefault(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isConfirmationEnabled()).isTrue();
        assertThat(result.isReminderEnabled()).isTrue();
        assertThat(result.isMarketingEnabled()).isTrue();
        assertThat(result.isUnsubscribedAll()).isFalse();
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void findByUserIdOrDefault_missingRow_returnsTransientDefault_withoutSaving() {
        UUID userId = UUID.randomUUID();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotificationPreference result = preferenceService.findByUserIdOrDefault(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isConfirmationEnabled()).isTrue();
        assertThat(result.isReminderEnabled()).isTrue();
        assertThat(result.isMarketingEnabled()).isTrue();
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void isAllowed_newUserWithNoRow_defaultsAllowedAndNeverWritesToDb() {
        UUID userId = UUID.randomUUID();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        boolean allowed = preferenceService.isAllowed(userId, NotificationType.CONFIRMATION);

        assertThat(allowed).isTrue();
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void isAllowed_unsubscribedAll_blocksEveryCategoryRegardlessOfIndividualFlags() {
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setUnsubscribedAll(true);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        assertThat(preferenceService.isAllowed(userId, NotificationType.CONFIRMATION)).isFalse();
        assertThat(preferenceService.isAllowed(userId, NotificationType.REMINDER)).isFalse();
        assertThat(preferenceService.isAllowed(userId, NotificationType.MARKETING)).isFalse();
    }

    @Test
    void isAllowed_perCategoryFlags_areCheckedIndependently() {
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setConfirmationEnabled(false);
        preference.setReminderEnabled(true);
        preference.setMarketingEnabled(false);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        assertThat(preferenceService.isAllowed(userId, NotificationType.CONFIRMATION)).isFalse();
        assertThat(preferenceService.isAllowed(userId, NotificationType.REMINDER)).isTrue();
        assertThat(preferenceService.isAllowed(userId, NotificationType.MARKETING)).isFalse();
    }

    @Test
    void updatePreferences_setsAllThreeCategoryFlagsFromRequest() {
        UUID userId = UUID.randomUUID();
        NotificationPreference existing = new NotificationPreference();
        existing.setUserId(userId);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setConfirmationEnabled(true);
        request.setReminderEnabled(false);
        request.setMarketingEnabled(false);

        NotificationPreference result = preferenceService.updatePreferences(userId, request);

        assertThat(result.isConfirmationEnabled()).isTrue();
        assertThat(result.isReminderEnabled()).isFalse();
        assertThat(result.isMarketingEnabled()).isFalse();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void unsubscribeAll_setsFlagTrue() {
        UUID userId = UUID.randomUUID();
        NotificationPreference existing = new NotificationPreference();
        existing.setUserId(userId);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        NotificationPreference result = preferenceService.unsubscribeAll(userId);

        assertThat(result.isUnsubscribedAll()).isTrue();
    }

    @Test
    void resubscribeAll_setsFlagFalse() {
        UUID userId = UUID.randomUUID();
        NotificationPreference existing = new NotificationPreference();
        existing.setUserId(userId);
        existing.setUnsubscribedAll(true);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        NotificationPreference result = preferenceService.resubscribeAll(userId);

        assertThat(result.isUnsubscribedAll()).isFalse();
    }
}
