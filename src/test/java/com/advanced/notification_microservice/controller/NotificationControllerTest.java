package com.advanced.notification_microservice.controller;

import com.advanced.notification_microservice.dto.NotificationRequest;
import com.advanced.notification_microservice.dto.NotificationResponse;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.excption.NotificationNotFoundException;
import com.advanced.notification_microservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private NotificationRequest validRequest() {
        return NotificationRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .email("jane@example.com")
                .phone("+1-555-0100")
                .confirmationCode("ABC123")
                .customerName("Jane Doe")
                .appointmentTime(LocalDateTime.now().plusDays(2))
                .treatmentName("Deep Tissue Massage")
                .build();
    }

    @Test
    void sendConfirmation_validRequest_returns202AndInvokesService() throws Exception {
        NotificationRequest request = validRequest();

        mockMvc.perform(post("/api/v1/notifications/confirmation")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(notificationService).sendConfirmation(any(NotificationRequest.class));
    }

    @Test
    void sendConfirmation_missingRequiredField_returns400WithFieldError() throws Exception {
        NotificationRequest request = validRequest();
        request.setBookingId(null);

        mockMvc.perform(post("/api/v1/notifications/confirmation")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.bookingId").exists());
    }

    @Test
    void sendConfirmation_malformedJsonBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/confirmation")
                        .contentType(APPLICATION_JSON)
                        .content("{ this is not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void cancelNotificationsForBooking_pathVariableNotAUuid_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/booking/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getHistory_userIdParamNotAUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/history").param("userId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void sendConfirmation_duplicateBooking_stillReturns202() throws Exception {
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(notificationService).sendConfirmation(any(NotificationRequest.class));

        mockMvc.perform(post("/api/v1/notifications/confirmation")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isAccepted());
    }

    @Test
    void cancelNotificationsForBooking_found_returns204() throws Exception {
        UUID bookingId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/notifications/booking/{bookingId}", bookingId))
                .andExpect(status().isNoContent());

        verify(notificationService).cancelNotificationsForBooking(eq(bookingId));
    }

    @Test
    void cancelNotificationsForBooking_notFound_returns404WithErrorBody() throws Exception {
        UUID bookingId = UUID.randomUUID();
        doThrow(new NotificationNotFoundException("No notifications found for booking: " + bookingId))
                .when(notificationService).cancelNotificationsForBooking(bookingId);

        mockMvc.perform(delete("/api/v1/notifications/booking/{bookingId}", bookingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No notifications found for booking: " + bookingId));
    }

    @Test
    void getHistory_noUserIdParam_callsServiceWithNull() throws Exception {
        when(notificationService.getHistory(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(notificationService).getHistory(isNull());
    }

    @Test
    void getHistory_withUserIdParam_callsServiceWithThatId_andReturnsBody() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationResponse response = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(NotificationStatus.SENT)
                .type(NotificationType.CONFIRMATION)
                .build();
        when(notificationService.getHistory(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/notifications/history").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].status").value("SENT"));

        verify(notificationService).getHistory(eq(userId));
    }
}
