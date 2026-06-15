package com.badyauniversity.eventbooking.controller;

import com.badyauniversity.eventbooking.model.Event;
import com.badyauniversity.eventbooking.repository.EventRepository;
import com.badyauniversity.eventbooking.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test-notifications")
public class NotificationTestController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/run-demo")
    public ResponseEntity<Map<String, String>> runDemo() {
        // 1. Create a sample event
        Event demoEvent = new Event();
        demoEvent.setTitle("Demo: AI Innovations 2026");
        demoEvent.setDescription("A special demo event for Computer Science and Engineering students.");
        demoEvent.setCategory("academic");
        demoEvent.setDate(LocalDate.now().plusDays(7));
        demoEvent.setTime(LocalTime.of(10, 0));
        demoEvent.setVenue("Innovation Hall");
        demoEvent.setCapacity(100);
        demoEvent.setPrice(BigDecimal.ZERO);
        demoEvent.setOrganizer("Tech Committee");
        demoEvent.setContactEmail("tech@badya.edu");
        demoEvent.setTargetFaculties("Computer Science,Engineering");
        
        Event savedEvent = eventRepository.save(demoEvent);

        // 2. Trigger notifications
        notificationService.sendEventAnnouncements(savedEvent);

        return ResponseEntity.ok(Map.of(
            "status", "Demo triggered",
            "event", savedEvent.getTitle(),
            "targets", savedEvent.getTargetFaculties(),
            "message", "Check backend console and notifications table for results."
        ));
    }
}
