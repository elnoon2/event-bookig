package com.badyauniversity.eventbooking.repository;

import com.badyauniversity.eventbooking.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByEventId(Long eventId);
    List<Notification> findByStudentId(Long studentId);
}
