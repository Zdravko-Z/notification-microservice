package com.advanced.notification_microservice.mapper;

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
}
