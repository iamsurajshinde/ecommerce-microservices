package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.model.Notification;
import com.ecommerce.notificationservice.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventIdAndChannel(String eventId, NotificationChannel channel);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
