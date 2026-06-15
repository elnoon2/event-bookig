package com.badyauniversity.eventbooking.service;

import com.badyauniversity.eventbooking.dto.BookingRequestDTO;
import com.badyauniversity.eventbooking.dto.BookingResponseDTO;
import com.badyauniversity.eventbooking.dto.EventDTO;
import com.badyauniversity.eventbooking.model.Booking;
import com.badyauniversity.eventbooking.model.Event;
import com.badyauniversity.eventbooking.repository.BookingRepository;
import com.badyauniversity.eventbooking.repository.EventRepository;
import com.badyauniversity.eventbooking.repository.UserRepository;
import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.repository.EventAttendanceRepository;
import com.badyauniversity.eventbooking.model.EventAttendance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for Booking business logic
 * Handles all booking-related operations
 */
@Service
@Transactional
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final NotificationService notificationService;
    private final AttendanceTokenService attendanceTokenService;
    private final EventAttendanceRepository eventAttendanceRepository;
    
    @Autowired
    public BookingService(BookingRepository bookingRepository, 
                          EventRepository eventRepository,
                          UserRepository userRepository,
                          EventService eventService,
                          NotificationService notificationService,
                          AttendanceTokenService attendanceTokenService,
                          EventAttendanceRepository eventAttendanceRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.notificationService = notificationService;
        this.attendanceTokenService = attendanceTokenService;
        this.eventAttendanceRepository = eventAttendanceRepository;
    }
    
    /**
     * Create a new booking
     * @param eventId The event ID
     * @param bookingRequest The booking request data
     * @return Created booking as DTO
     * @throws RuntimeException if event not found or capacity exceeded
     */
    public BookingResponseDTO createBooking(Long eventId, BookingRequestDTO bookingRequest) {
        // Enforce guest check: user must be logged in/registered
        User student = userRepository.findByEmail(bookingRequest.getStudentEmail())
                .orElseThrow(() -> new RuntimeException("Please login first to book an event."));

        // Get the event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        
        // Prevent duplicate bookings
        boolean alreadyBooked = bookingRepository.existsByEvent_IdAndStudentEmail(eventId, bookingRequest.getStudentEmail());
        if (alreadyBooked) {
            throw new RuntimeException("You have already booked this event.");
        }

        // Check available capacity
        Integer totalBookedTickets = bookingRepository.countTotalTicketsByEventId(eventId);
        if (totalBookedTickets == null) {
            totalBookedTickets = 0;
        }
        
        Integer availableCapacity = event.getCapacity() - totalBookedTickets;
        
        if (availableCapacity <= 0) {
            throw new RuntimeException("This event is sold out. No tickets remaining.");
        }
        if (bookingRequest.getTickets() > availableCapacity) {
            throw new RuntimeException(
                String.format("Not enough tickets left. Available: %d, requested: %d.",
                    availableCapacity, bookingRequest.getTickets())
            );
        }
        
        BigDecimal unitPrice = event.getPrice() != null ? event.getPrice() : BigDecimal.ZERO;
        boolean isPaid = unitPrice.compareTo(BigDecimal.ZERO) > 0;
        String paymentMethod = null;
        if (isPaid) {
            String pm = bookingRequest.getPaymentMethod();
            if (pm == null || pm.isBlank()) {
                throw new RuntimeException("Payment method is required for paid events. Choose Visa or Cash.");
            }
            String normalized = pm.trim().toUpperCase();
            if (!"VISA".equals(normalized) && !"CASH".equals(normalized)) {
                throw new RuntimeException("Invalid payment method. Use VISA or CASH.");
            }
            paymentMethod = normalized;
        }
        
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(bookingRequest.getTickets()));
        
        Booking booking = new Booking();
        booking.setEvent(event);
        booking.setStudentName(bookingRequest.getStudentName());
        booking.setStudentId(bookingRequest.getStudentId());
        booking.setStudentEmail(bookingRequest.getStudentEmail());
        booking.setTickets(bookingRequest.getTickets());
        booking.setTotalAmount(totalAmount);
        booking.setPaymentMethod(paymentMethod);
        booking.setTicketStatus("ACTIVE");
        
        // Save first to get the database-generated ID
        Booking savedBooking = bookingRepository.save(booking);
        
        // Generate secure QR payload: TICKET:id|USER:userId|EVENT:eventId|TOKEN:random
        String secureToken = UUID.randomUUID().toString().replace("-", "");
        String qrPayload = String.format("TICKET:%d|USER:%d|EVENT:%d|TOKEN:%s", 
                savedBooking.getId(), student.getId(), event.getId(), secureToken);
        
        savedBooking.setQrToken(qrPayload);
        // Save again to persist the token
        savedBooking = bookingRepository.save(savedBooking);
        
        // --- WhatsApp Notification Integration ---
        try {
            // Send the permanent ticket QR token instead of a temporary attendance token
            notificationService.sendBookingConfirmation(student, event, savedBooking.getQrToken());
        } catch (Exception e) {
            System.err.println("Failed to send booking WhatsApp: " + e.getMessage());
        }

        // Convert to response DTO
        return convertToResponseDTO(savedBooking);
    }

    /**
     * Scans and validates a ticket QR code. Marks it as USED if valid.
     */
    public BookingResponseDTO scanTicket(String qrToken, Long eventId, String adminUsername) {
        if (qrToken == null || qrToken.isBlank()) {
            throw new RuntimeException("Invalid QR code scanned.");
        }
        
        Booking booking = bookingRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new RuntimeException("Ticket does not exist."));
                
        if (eventId != null && !booking.getEvent().getId().equals(eventId)) {
            throw new RuntimeException("Ticket belongs to a different event: " + booking.getEvent().getTitle());
        }
        
        if ("USED".equalsIgnoreCase(booking.getTicketStatus())) {
            throw new RuntimeException("Ticket already used.");
        }
        
        if (!"ACTIVE".equalsIgnoreCase(booking.getTicketStatus())) {
            throw new RuntimeException("Ticket is inactive.");
        }
        
        // Mark ticket as USED, save scan timestamp and admin ID
        booking.setTicketStatus("USED");
        booking.setScannedAt(LocalDateTime.now());
        booking.setScannedBy(adminUsername != null ? adminUsername : "Admin");
        
        Booking savedBooking = bookingRepository.save(booking);

        // Record attendance if the student has an account
        if (booking.getStudentEmail() != null) {
            userRepository.findByEmail(booking.getStudentEmail()).ifPresent(user -> {
                if (!eventAttendanceRepository.findByEvent_IdAndUser_Id(booking.getEvent().getId(), user.getId()).isPresent()) {
                    EventAttendance attendance = new EventAttendance();
                    attendance.setEvent(booking.getEvent());
                    attendance.setUser(user);
                    attendance.setCheckedInAt(LocalDateTime.now());
                    eventAttendanceRepository.save(attendance);
                }
            });
        }
        
        return convertToResponseDTO(savedBooking);
    }
    
    /**
     * Get all bookings
     * @return List of all bookings as DTOs
     */
    public List<BookingResponseDTO> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return bookings.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get booking by ID
     * @param id The booking ID
     * @return Booking DTO if found
     * @throws RuntimeException if booking not found
     */
    public BookingResponseDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        return convertToResponseDTO(booking);
    }
    
    /**
     * Get bookings by student email
     * @param studentEmail The student's email
     * @return List of bookings made by the student
     */
    public List<BookingResponseDTO> getBookingsByStudentEmail(String studentEmail) {
        List<Booking> bookings = bookingRepository.findByStudentEmail(studentEmail);
        return bookings.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get bookings by event ID
     * @param eventId The event ID
     * @return List of bookings for the event
     */
    public List<BookingResponseDTO> getBookingsByEventId(Long eventId) {
        List<Booking> bookings = bookingRepository.findByEventId(eventId);
        return bookings.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete a booking
     * @param id The booking ID
     * @throws RuntimeException if booking not found
     */
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        
        try {
            User student = userRepository.findByEmail(booking.getStudentEmail()).orElse(null);
            if (student != null) {
                notificationService.sendBookingCancellation(student, booking.getEvent(), booking.getTickets());
            } else {
                System.err.println("Cannot send cancellation WhatsApp: student with email " + booking.getStudentEmail() + " not found in DB.");
            }
        } catch (Exception e) {
            System.err.println("Failed to send booking cancellation WhatsApp: " + e.getMessage());
        }
        
        bookingRepository.delete(booking);
    }
    
    /**
     * Get available capacity for an event
     * @param eventId The event ID
     * @return Available capacity
     */
    public Integer getAvailableCapacity(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        
        Integer totalBookedTickets = bookingRepository.countTotalTicketsByEventId(eventId);
        if (totalBookedTickets == null) {
            totalBookedTickets = 0;
        }
        
        return event.getCapacity() - totalBookedTickets;
    }
    
    /**
     * Convert Booking entity to BookingResponseDTO
     * @param booking The booking entity
     * @return BookingResponseDTO
     */
    private BookingResponseDTO convertToResponseDTO(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setEventId(booking.getEvent().getId());
        
        // Convert event to DTO
        EventDTO eventDTO = eventService.getEventById(booking.getEvent().getId());
        dto.setEvent(eventDTO);
        
        dto.setStudentName(booking.getStudentName());
        dto.setStudentId(booking.getStudentId());
        dto.setStudentEmail(booking.getStudentEmail());
        dto.setTickets(booking.getTickets());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setBookingDate(booking.getBookingDate());
        dto.setPaymentMethod(booking.getPaymentMethod());
        dto.setQrToken(booking.getQrToken());
        dto.setTicketStatus(booking.getTicketStatus());
        dto.setScannedAt(booking.getScannedAt());
        dto.setScannedBy(booking.getScannedBy());
        
        return dto;
    }
}

