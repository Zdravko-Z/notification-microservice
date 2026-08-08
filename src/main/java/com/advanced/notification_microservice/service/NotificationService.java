package com.advanced.notification_microservice.service;

import com.advanced.notification_microservice.dto.NotificationResponse;
import com.advanced.notification_microservice.entity.Notification;
import com.advanced.notification_microservice.entity.NotificationStatus;
import com.advanced.notification_microservice.entity.NotificationType;
import com.advanced.notification_microservice.dto.NotificationRequest;
import com.advanced.notification_microservice.excption.NotificationNotFoundException;
import com.advanced.notification_microservice.mapper.NotificationMapper;
import com.advanced.notification_microservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Sort SENT_AT_DESC = Sort.by(Sort.Direction.DESC, "sentAt");
    private static final String REMINDER_SUBJECT = "Your appointment is coming up - Serenity Spa";
    private final NotificationRepository notificationRepository;
    private final AsyncNotificationService asyncNotificationService;
    private final NotificationPreferenceService notificationPreferenceService;

    public void sendConfirmation(NotificationRequest request) {
        String subject = "Booking confirmed - Serenity Spa";
        String body = buildConfirmationEmailBody(request);

        Notification notification = NotificationMapper.toEntity(request, subject, body, NotificationType.CONFIRMATION);

        persistAndDispatch(notification, request.getUserId(), NotificationType.CONFIRMATION);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendReminderForBooking(UUID confirmationNotificationId) {
        Notification confirmation = notificationRepository.findById(confirmationNotificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "There is no notification with this id: " + confirmationNotificationId));

        if (confirmation.isReminderSent()) {
            return;
        }

        try {
            String body = buildReminderEmailBody(confirmation.getCustomerName(), confirmation.getTreatmentName(),
                    confirmation.getAppointmentTime(), confirmation.getConfirmationCode());
            Notification reminder = NotificationMapper.toReminderEntity(confirmation, REMINDER_SUBJECT, body);

            persistAndDispatch(reminder, confirmation.getUserId(), NotificationType.REMINDER);
        } catch (DataIntegrityViolationException e) {
            log.warn("Reminder already exists for booking {}, skipping", confirmation.getBookingId());
        } finally {
            confirmation.setReminderSent(true);
        }
    }

    public void cancelNotificationsForBooking(UUID bookingId) {
        List<Notification> notifications = notificationRepository.findByBookingId(bookingId);

        if (notifications.isEmpty()) {
            throw new NotificationNotFoundException("No notifications found for booking: " + bookingId);
        }

        for (Notification notification : notifications) {
            notification.setCancelled(true);

            if (notification.getStatus() == NotificationStatus.PENDING
                    || notification.getStatus() == NotificationStatus.FAILED) {
                notification.setStatus(NotificationStatus.CANCELLED);
            }
        }

        log.info("Cancelled {} notification(s) for booking: {}", notifications.size(), bookingId);
    }

    public List<NotificationResponse> getHistory(UUID userId){
        List<Notification> notifications = (userId == null) ?
                notificationRepository.findAll(SENT_AT_DESC) :
                notificationRepository.findByUserId(userId, SENT_AT_DESC);

        return notifications.stream().map(NotificationMapper::toResponse).toList();
    }

    private void persistAndDispatch(Notification notification, UUID userId, NotificationType type) {
        boolean allowed = notificationPreferenceService.isAllowed(userId, type);
        if (!allowed) {
            notification.setStatus(NotificationStatus.SUPPRESSED);
        }

        notificationRepository.save(notification);

        if (allowed) {
            log.info("Saved {} notification for booking: {}", type, notification.getBookingId());
            asyncNotificationService.sendEmailAsync(notification.getId());
        } else {
            log.info("{} notification suppressed by preference for user {}, booking {}",
                    type, userId, notification.getBookingId());
        }
    }

    private String buildConfirmationEmailBody(NotificationRequest request) {
        return buildEmailBody(
                "WELCOME TO",
                "Your appointment has been confirmed. We look forward to welcoming you.",
                "Thank you for choosing us. We look forward to seeing you.",
                request.getCustomerName(), request.getTreatmentName(),
                request.getAppointmentTime(), request.getConfirmationCode());
    }

    private String buildReminderEmailBody(String customerName, String treatmentName,
                                          LocalDateTime appointmentTime, String confirmationCode) {
        return buildEmailBody(
                "REMINDER FROM",
                "This is a friendly reminder that your appointment is coming up soon. We look forward to welcoming you and providing you with an exceptional spa experience.",
                "See you soon.",
                customerName, treatmentName, appointmentTime, confirmationCode);
    }

    private String buildEmailBody(String headerLabel, String introMessage, String footerMessage,
                                  String customerName, String treatmentName,
                                  LocalDateTime appointmentTime, String confirmationCode) {
        String formattedDate = appointmentTime.format(DATE_FORMATTER);
        String formattedTime = appointmentTime.format(TIME_FORMATTER);

        String template = """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/></head>
            <body style="margin:0;padding:0;background:#f7f4ef;font-family:Georgia,serif;color:#1c1917;">

            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f7f4ef;padding:40px 20px;">
            <tr><td align="center">

            <table role="presentation" width="600" cellspacing="0" cellpadding="0"
                   style="max-width:600px;background:#ffffff;border:1px solid #e5e0d8;overflow:hidden;">

                <!-- Header -->
                <tr>
                    <td align="center" style="background:#1c1917;padding:36px 30px;">
                        <p style="margin:0 0 4px;font-size:11px;letter-spacing:0.25em;text-transform:uppercase;color:#b8973a;">
                            %s
                        </p>
                        <h1 style="margin:0;font-size:28px;color:#d4af7a;font-weight:300;letter-spacing:0.18em;font-family:Georgia,serif;">
                            SERENITY SPA
                        </h1>
                    </td>
                </tr>

                <!-- Greeting -->
                <tr>
                    <td style="padding:40px 44px 16px;">
                        <h2 style="margin:0;font-size:24px;color:#1c1917;font-weight:300;font-family:Georgia,serif;">
                            Hello, %s
                        </h2>
                    </td>
                </tr>

                <!-- Message -->
                <tr>
                    <td style="padding:0 44px 28px;">
                        <p style="margin:0;font-size:15px;line-height:26px;color:#78716c;font-family:Arial,sans-serif;">
                            %s
                        </p>
                    </td>
                </tr>

                <!-- Appointment Card -->
                <tr>
                    <td style="padding:0 44px 36px;">
                        <table width="100%%" cellspacing="0" cellpadding="0"
                               style="background:#f7f4ef;border:1px solid #e5e0d8;">
                            <tr>
                                <td style="padding:6px 0;background:#b8973a;" colspan="2"></td>
                            </tr>
                            <tr>
                                <td style="padding:24px 28px;">
                                    <table width="100%%">
                                        <tr>
                                            <td style="padding-bottom:20px;">
                                                <div style="font-size:11px;color:#78716c;text-transform:uppercase;letter-spacing:0.12em;margin-bottom:6px;font-family:Arial,sans-serif;">
                                                    Treatment
                                                </div>
                                                <div style="font-size:17px;color:#1c1917;font-family:Georgia,serif;">
                                                    %s
                                                </div>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <table width="100%%">
                                                    <tr>
                                                        <td width="50%%">
                                                            <div style="font-size:11px;color:#78716c;text-transform:uppercase;letter-spacing:0.12em;margin-bottom:6px;font-family:Arial,sans-serif;">
                                                                Date
                                                            </div>
                                                            <div style="font-size:17px;color:#1c1917;font-family:Georgia,serif;">
                                                                %s
                                                            </div>
                                                        </td>
                                                        <td width="50%%">
                                                            <div style="font-size:11px;color:#78716c;text-transform:uppercase;letter-spacing:0.12em;margin-bottom:6px;font-family:Arial,sans-serif;">
                                                                Time
                                                            </div>
                                                            <div style="font-size:17px;color:#1c1917;font-family:Georgia,serif;">
                                                                %s
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>

                <!-- Confirmation Code -->
                <tr>
                    <td style="padding:0 44px 36px;">
                        <p style="margin:0 0 8px;font-size:11px;color:#78716c;text-transform:uppercase;letter-spacing:0.12em;font-family:Arial,sans-serif;">
                            Confirmation Code
                        </p>
                        <p style="margin:0;font-size:22px;color:#b8973a;letter-spacing:0.15em;font-family:Georgia,serif;">
                            %s
                        </p>
                    </td>
                </tr>

                <!-- Divider -->
                <tr>
                    <td style="padding:0 44px 36px;">
                        <div style="height:1px;background:#e5e0d8;"></div>
                    </td>
                </tr>

                <!-- Footer -->
                <tr>
                    <td align="center" style="background:#1c1917;padding:28px 30px;">
                        <p style="margin:0 0 6px;font-size:11px;letter-spacing:0.15em;text-transform:uppercase;color:#b8973a;font-family:Arial,sans-serif;">
                            SERENITY SPA
                        </p>
                        <p style="margin:0;font-size:13px;color:#78716c;font-family:Arial,sans-serif;line-height:22px;">
                            %s
                        </p>
                    </td>
                </tr>

            </table>

            </td></tr>
            </table>

            </body>
            </html>
            """;
        return String.format(template,
                headerLabel,
                customerName,
                introMessage,
                treatmentName,
                formattedDate,
                formattedTime,
                confirmationCode,
                footerMessage);
    }
}
