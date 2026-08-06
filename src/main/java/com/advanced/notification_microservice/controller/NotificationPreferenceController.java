package com.advanced.notification_microservice.controller;

import com.advanced.notification_microservice.dto.NotificationPreferenceRequest;
import com.advanced.notification_microservice.dto.NotificationPreferenceResponse;
import com.advanced.notification_microservice.entity.NotificationPreference;
import com.advanced.notification_microservice.mapper.NotificationPreferenceMapper;
import com.advanced.notification_microservice.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {
    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping("/{userId}")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(@PathVariable UUID userId) {
        NotificationPreference preference = notificationPreferenceService.getOrCreateDefault(userId);
        return ResponseEntity.ok(NotificationPreferenceMapper.toResponse(preference));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @PathVariable UUID userId, @Valid @RequestBody NotificationPreferenceRequest request) {
        log.info("Updating notification preferences for user: {}", userId);

        NotificationPreference preference = notificationPreferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(NotificationPreferenceMapper.toResponse(preference));
    }

    @PostMapping("/{userId}/unsubscribe")
    public ResponseEntity<NotificationPreferenceResponse> unsubscribe(@PathVariable UUID userId) {
        log.info("Unsubscribing user {} from all notifications", userId);

        NotificationPreference preference = notificationPreferenceService.unsubscribeAll(userId);
        return ResponseEntity.ok(NotificationPreferenceMapper.toResponse(preference));
    }

    @PostMapping("/{userId}/resubscribe")
    public ResponseEntity<NotificationPreferenceResponse> resubscribe(@PathVariable UUID userId) {
        log.info("Resubscribing user {} to notifications", userId);

        NotificationPreference preference = notificationPreferenceService.resubscribeAll(userId);
        return ResponseEntity.ok(NotificationPreferenceMapper.toResponse(preference));
    }
}
