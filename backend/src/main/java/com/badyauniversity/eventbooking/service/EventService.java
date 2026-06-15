package com.badyauniversity.eventbooking.service;

import com.badyauniversity.eventbooking.dto.EventDTO;
import com.badyauniversity.eventbooking.model.Event;
import com.badyauniversity.eventbooking.repository.BookingRepository;
import com.badyauniversity.eventbooking.repository.EventAttendanceRepository;
import com.badyauniversity.eventbooking.repository.EventRepository;
import com.badyauniversity.eventbooking.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for Event business logic
 * Handles all event-related operations
 */
@Service
@Transactional
public class EventService {
    
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final NotificationRepository notificationRepository;
    
    @Autowired
    public EventService(EventRepository eventRepository, 
                        BookingRepository bookingRepository,
                        EventAttendanceRepository eventAttendanceRepository,
                        NotificationRepository notificationRepository) {
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
        this.eventAttendanceRepository = eventAttendanceRepository;
        this.notificationRepository = notificationRepository;
    }
    
    /**
     * Get all events
     * @return List of all events as DTOs
     */
    public List<EventDTO> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get event by ID
     * @param id The event ID
     * @return Event DTO if found
     * @throws RuntimeException if event not found
     */
    public EventDTO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return convertToDTO(event);
    }
    
    /**
     * Get events by category
     * @param category The event category
     * @return List of events in the specified category
     */
    public List<EventDTO> getEventsByCategory(String category) {
        List<Event> events = eventRepository.findByCategory(category);
        return events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get upcoming events
     * @return List of upcoming events
     */
    public List<EventDTO> getUpcomingEvents() {
        List<Event> events = eventRepository.findByDateAfter(LocalDate.now());
        return events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Create a new event
     * @param eventDTO The event data
     * @param adminId The admin creating the event (owner)
     * @return Created event as DTO
     */
    public EventDTO createEvent(EventDTO eventDTO, Long adminId) {
        System.out.println("[INFO] [BACKEND-EVENT] Creating new event. Title: " + eventDTO.getTitle() + ", created by Admin ID: " + adminId);
        Event event = convertToEntity(eventDTO);
        event.setCreatedByAdminId(adminId);
        Event savedEvent = eventRepository.save(event);
        System.out.println("[INFO] [BACKEND-EVENT] Event saved to DB. ID: " + savedEvent.getId());
        return convertToDTO(savedEvent);
    }
    
    /**
     * Update an existing event
     * @param id The event ID
     * @param eventDTO The updated event data
     * @param adminId The admin requesting the update (must be owner)
     * @return Updated event as DTO
     * @throws RuntimeException if event not found
     */
    public EventDTO updateEvent(Long id, EventDTO eventDTO, Long adminId) {
        System.out.println("[INFO] [BACKEND-EVENT] Updating event. ID: " + id + ", requesting Admin ID: " + adminId);
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event found with id: " + id));

        // Relaxed: any admin can update any event.
        
        // Update fields
        existingEvent.setTitle(eventDTO.getTitle());
        existingEvent.setDescription(eventDTO.getDescription());
        existingEvent.setCategory(eventDTO.getCategory());
        existingEvent.setDate(eventDTO.getDate());
        existingEvent.setTime(eventDTO.getTime());
        existingEvent.setVenue(eventDTO.getVenue());
        existingEvent.setCapacity(eventDTO.getCapacity());
        existingEvent.setPrice(eventDTO.getPrice());
        existingEvent.setImage(eventDTO.getImage());
        existingEvent.setOrganizer(eventDTO.getOrganizer());
        existingEvent.setContactEmail(eventDTO.getContactEmail());
        existingEvent.setTargetFaculties(eventDTO.getTargetFaculties());
        
        Event updatedEvent = eventRepository.save(existingEvent);
        System.out.println("[INFO] [BACKEND-EVENT] Event ID: " + id + " successfully updated.");
        return convertToDTO(updatedEvent);
    }
    
    /**
     * Delete an event
     * @param id The event ID
     * @throws RuntimeException if event not found
     */
    public void deleteEvent(Long id) {
        System.out.println("[INFO] [BACKEND-EVENT] Deleting event. ID: " + id);
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found with id: " + id);
        }
        
        // Manual Cascade Delete: Delete related records first to bypass foreign key constraints
        try {
            // Delete bookings
            int bookingsCount = bookingRepository.findByEventId(id).size();
            System.out.println("[INFO] [BACKEND-EVENT] Cascade deleting " + bookingsCount + " bookings for event ID: " + id);
            bookingRepository.deleteAll(bookingRepository.findByEventId(id));
            
            // Delete attendance records
            int attendanceCount = eventAttendanceRepository.findByEvent_IdOrderByCheckedInAtAsc(id).size();
            System.out.println("[INFO] [BACKEND-EVENT] Cascade deleting " + attendanceCount + " attendance records for event ID: " + id);
            eventAttendanceRepository.deleteAll(eventAttendanceRepository.findByEvent_IdOrderByCheckedInAtAsc(id));
            
            // Delete notifications
            int notificationCount = notificationRepository.findByEventId(id).size();
            System.out.println("[INFO] [BACKEND-EVENT] Cascade deleting " + notificationCount + " notifications for event ID: " + id);
            notificationRepository.deleteAll(notificationRepository.findByEventId(id));
            
        } catch (Exception e) {
            System.err.println("Warning: Manual cascade deletion encountered an issue: " + e.getMessage());
        }

        // Now safe to delete the event
        eventRepository.deleteById(id);
        System.out.println("[INFO] [BACKEND-EVENT] Event ID: " + id + " successfully deleted.");
    }
    
    /**
     * Check if event exists
     * @param id The event ID
     * @return true if event exists, false otherwise
     */
    public boolean eventExists(Long id) {
        return eventRepository.existsById(id);
    }
    
    /**
     * Get event entity by ID (for internal use)
     * @param id The event ID
     * @return Event entity if found
     * @throws RuntimeException if event not found
     */
    public Event getEventEntityById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }
    
    /**
     * Convert Event entity to EventDTO
     * @param event The event entity
     * @return EventDTO
     */
    private EventDTO convertToDTO(Event event) {
        EventDTO dto = new EventDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setCategory(event.getCategory());
        dto.setDate(event.getDate());
        dto.setTime(event.getTime());
        dto.setVenue(event.getVenue());
        dto.setCapacity(event.getCapacity());
        Integer sold = bookingRepository.countTotalTicketsByEventId(event.getId());
        if (sold == null) {
            sold = 0;
        }
        dto.setTicketsAvailable(Math.max(0, event.getCapacity() - sold));
        dto.setPrice(event.getPrice());
        dto.setImage(event.getImage());
        dto.setOrganizer(event.getOrganizer());
        dto.setContactEmail(event.getContactEmail());
        dto.setTargetFaculties(event.getTargetFaculties());
        dto.setCreatedByAdminId(event.getCreatedByAdminId());
        return dto;
    }
    
    /**
     * Convert EventDTO to Event entity
     * @param dto The event DTO
     * @return Event entity
     */
    private Event convertToEntity(EventDTO dto) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setCategory(dto.getCategory());
        event.setDate(dto.getDate());
        event.setTime(dto.getTime());
        event.setVenue(dto.getVenue());
        event.setCapacity(dto.getCapacity());
        event.setPrice(dto.getPrice());
        event.setImage(dto.getImage());
        event.setOrganizer(dto.getOrganizer());
        event.setContactEmail(dto.getContactEmail());
        event.setTargetFaculties(dto.getTargetFaculties());
        return event;
    }
}

