package com.advanced.notification_microservice.service;

import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.dto.NotificationRequest;
import com.advanced.notification_microservice.mapper.NotificationMapper;
import com.advanced.notification_microservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final AsyncNotificationService asyncNotificationService;

    public void sendConfirmation(NotificationRequest request) {
        String subject = "Booking confirmed - Serenity Spa";
        String body = buildEmailBody(request);

        Notification notification = NotificationMapper.toEntity(request, subject, body, NotificationType.CONFIRMATION);
        notificationRepository.save(notification);

        log.info("Saved confirmation notification for booking: {}", request.getBookingId());

        asyncNotificationService.sendEmailAsync(notification.getId());
    }


    //TODO
    public void sendReminder(NotificationRequest request){

    }

    private String buildEmailBody(NotificationRequest request) {

        String template = """
                <html><body>
                <h1>Hello, %s</h1>
                
                <p>Your<strong> %s</strong> is confirmed for <strong>%s</strong>.</p>
                <p>We are looking forward to seeing you at Serenity Spa.</p>
                </body></html>
                """;

        return String.format(template,
                request.getCustomerName(),
                request.getTreatmentName(),
                request.getAppointmentTime().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm")));
    }
}
