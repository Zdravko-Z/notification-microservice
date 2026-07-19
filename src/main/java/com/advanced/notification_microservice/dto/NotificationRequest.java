package com.advanced.notification_microservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class NotificationRequest {

    @NotNull(message = "bookingId cannot be null")
    private UUID bookingId;

    @NotNull(message = "userId cannot be null")
    private UUID userId;

    @NotBlank(message = "email cannot be blank")
    private String email;

    private String phone;

    @NotBlank(message = "confirmation code cannot be blank")
    private String confirmationCode;

    @NotBlank(message = "customerName cannot be blank")
    private String customerName;

    @NotNull(message = "appointmentTime cannot be null")
    private LocalDateTime appointmentTime;

    @NotBlank(message = "serviceName cannot be blank")
    private String treatmentName;
}
