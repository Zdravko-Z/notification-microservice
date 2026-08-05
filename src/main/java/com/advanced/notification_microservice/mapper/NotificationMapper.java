package com.advanced.notification_microservice.mapper;

import com.advanced.notification_microservice.dto.NotificationResponse;
import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.dto.NotificationRequest;

public class NotificationMapper {
    public static Notification toEntity(NotificationRequest request, String subject, String body, NotificationType type){
        Notification notification = new Notification();
        notification.setBookingId(request.getBookingId());
        notification.setRecipientEmail(request.getEmail());
        notification.setRecipientPhone(request.getPhone());
        notification.setUserId(request.getUserId());
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setType(type);
        notification.setRetryCount(0);

        return notification;
    }

    public static NotificationResponse toResponse(Notification notification){
        return NotificationResponse.builder()
                .id(notification.getId())
                .bookingId(notification.getBookingId())
                .userId(notification.getUserId())
                .recipientEmail(notification.getRecipientEmail())
                .recipientPhone(notification.getRecipientPhone())
                .subject(notification.getSubject())
                .status(notification.getStatus())
                .type(notification.getType())
                .retryCount(notification.getRetryCount())
                .sentAt(notification.getSentAt())
                .build();
    }
}
