package com.advanced.notification_microservice.controller;

import com.advanced.notification_microservice.dto.NotificationPreferenceRequest;
import com.advanced.notification_microservice.entity.NotificationPreference;
import com.advanced.notification_microservice.service.NotificationPreferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(NotificationPreferenceController.class)
class NotificationPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationPreferenceService notificationPreferenceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private NotificationPreference preferenceFor(UUID userId) {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        return preference;
    }

    @Test
    void getPreferences_returns200WithBody() throws Exception {
        UUID userId = UUID.randomUUID();
        when(notificationPreferenceService.getOrCreateDefault(userId)).thenReturn(preferenceFor(userId));

        mockMvc.perform(get("/api/v1/notifications/preferences/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.confirmationEnabled").value(true))
                .andExpect(jsonPath("$.unsubscribedAll").value(false));
    }

    @Test
    void updatePreferences_validRequest_returns200AndInvokesService() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setConfirmationEnabled(true);
        request.setReminderEnabled(false);
        request.setMarketingEnabled(false);

        when(notificationPreferenceService.updatePreferences(eq(userId), any(NotificationPreferenceRequest.class)))
                .thenReturn(preferenceFor(userId));

        mockMvc.perform(put("/api/v1/notifications/preferences/{userId}", userId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationPreferenceService).updatePreferences(eq(userId), any(NotificationPreferenceRequest.class));
    }

    @Test
    void updatePreferences_missingField_returns400WithFieldError() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setConfirmationEnabled(true);
        request.setReminderEnabled(null);
        request.setMarketingEnabled(false);

        mockMvc.perform(put("/api/v1/notifications/preferences/{userId}", userId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.reminderEnabled").exists());
    }

    @Test
    void unsubscribeAll_returns200AndInvokesService() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = preferenceFor(userId);
        preference.setUnsubscribedAll(true);
        when(notificationPreferenceService.unsubscribeAll(userId)).thenReturn(preference);

        mockMvc.perform(post("/api/v1/notifications/preferences/unsubscribe/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unsubscribedAll").value(true));

        verify(notificationPreferenceService).unsubscribeAll(eq(userId));
    }

    @Test
    void resubscribeAll_returns200AndInvokesService() throws Exception {
        UUID userId = UUID.randomUUID();
        when(notificationPreferenceService.resubscribeAll(userId)).thenReturn(preferenceFor(userId));

        mockMvc.perform(post("/api/v1/notifications/preferences/resubscribe/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unsubscribedAll").value(false));

        verify(notificationPreferenceService).resubscribeAll(eq(userId));
    }
}
