package com.advanced.notification_microservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "confirmation_enabled", nullable = false)
    private boolean confirmationEnabled = true;

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled = true;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled = true;

    @Column(name = "unsubscribed_all", nullable = false)
    private boolean unsubscribedAll = false;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
