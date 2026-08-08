package com.advanced.notification_microservice.controller;

import com.advanced.notification_microservice.dto.NotificationRequest;
import com.advanced.notification_microservice.dto.NotificationResponse;
import com.advanced.notification_microservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping("/confirmation")
    public ResponseEntity<Void> sendConfirmation(@Valid @RequestBody NotificationRequest request) {
        log.info("Received confirmation request for booking ID: {}", request.getBookingId());

        try {
            notificationService.sendConfirmation(request);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate confirmation request for booking id: {}", request.getBookingId());
        }
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/booking/{bookingId}")
    public ResponseEntity<Void> cancelNotification(@PathVariable UUID bookingId){
        log.info("Received request to cancel notifications for booking id: {}", bookingId);

        notificationService.cancelNotificationsForBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<NotificationResponse>> getHistory(@RequestParam(required = false)UUID userId) {
        log.info("Getting notification history {} " + userId != null ? "for user id: " + userId : "for all users");
        List<NotificationResponse> history = notificationService.getHistory(userId);

        return ResponseEntity.ok(history);
    }
}
