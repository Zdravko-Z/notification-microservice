package com.advanced.notification_microservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationPreferenceRequest {
    @NotNull(message = "ConfirmationEnabled cannot be empty")
    private Boolean confirmationEnabled;

    @NotNull(message = "reminderEnabled cannot be null")
    private Boolean reminderEnabled;

    @NotNull(message = "marketingEnabled cannot be null")
    private Boolean marketingEnabled;
}
