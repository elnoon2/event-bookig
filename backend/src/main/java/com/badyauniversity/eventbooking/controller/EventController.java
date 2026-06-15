package com.badyauniversity.eventbooking.controller;

import com.badyauniversity.eventbooking.dto.EventDTO;
import com.badyauniversity.eventbooking.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Event operations
 * Handles HTTP requests related to events
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {
    
    private final EventService eventService;
    
    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }
    
    /**
     * Get all events
     * GET /api/events
     * @return List of all events
     */
    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        List<EventDTO> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get event by ID
     * GET /api/events/{id}
     * @param id The event ID
     * @return Event DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable Long id) {
        EventDTO event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }
    
    /**
     * Get events by category
     * GET /api/events/category/{category}
     * @param category The event category
     * @return List of events in the category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<EventDTO>> getEventsByCategory(@PathVariable String category) {
        List<EventDTO> events = eventService.getEventsByCategory(category);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get upcoming events
     * GET /api/events/upcoming
     * @return List of upcoming events
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventDTO>> getUpcomingEvents() {
        List<EventDTO> events = eventService.getUpcomingEvents();
        return ResponseEntity.ok(events);
    }
}

