package com.advanced.notification_microservice.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    public void sendEmail(String recipientEmail, String subject, String body){
        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(recipientEmail)
                    .subject(subject)
                    .html(body)
                    .build();

            CreateEmailResponse response = resend.emails().send(options);
            log.info("Email was sent successfully to {} with ID {}", recipientEmail, response.getId());
        } catch (ResendException e){
            log.error("Failed to send email: {}", e.getMessage());
            throw new RuntimeException("Failed to send email" ,e);
        }
    }
}
