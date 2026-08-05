package com.advanced.notification_microservice.repository;

import com.advanced.notification_microservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderBySentAtDesc(UUID userId);

    List<Notification> findAllByOrderBySentAtDesc();
}
