package com.advanced.notification_microservice.controller;

import com.advanced.notification_microservice.dto.NotificationRequest;
import com.advanced.notification_microservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping("/confirmation")
    public ResponseEntity<Void> sendConfirmation(@Valid @RequestBody NotificationRequest request) {
        log.info("Received confirmation request for booking ID: {}", request.getBookingId());

        try {
            notificationService.sendConfirmation(request);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate confirmation request for booking ID: {}", request.getBookingId());
        }
        return ResponseEntity.accepted().build();
    }
}
