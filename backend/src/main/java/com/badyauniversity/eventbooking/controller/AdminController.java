package com.badyauniversity.eventbooking.controller;

import com.badyauniversity.eventbooking.dto.AdminRequestDTO;
import com.badyauniversity.eventbooking.dto.AdminResponseDTO;
import com.badyauniversity.eventbooking.dto.AttendanceRowDTO;
import com.badyauniversity.eventbooking.dto.BookingRequestDTO;
import com.badyauniversity.eventbooking.dto.BookingResponseDTO;
import com.badyauniversity.eventbooking.dto.EventDTO;
import com.badyauniversity.eventbooking.model.Admin;
import com.badyauniversity.eventbooking.model.Event;
import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.service.AdminService;
import com.badyauniversity.eventbooking.service.UserService;
import com.badyauniversity.eventbooking.service.AttendanceService;
import com.badyauniversity.eventbooking.service.BookingService;
import com.badyauniversity.eventbooking.service.EventService;
import com.badyauniversity.eventbooking.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Admin operations
 * Handles HTTP requests related to admin functionality
 * Admins can create events and make bookings like regular users
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private final AdminService adminService;
    private final EventService eventService;
    private final BookingService bookingService;
    private final AttendanceService attendanceService;
    private final UserService userService;
    private final NotificationService notificationService;
    
    @Autowired
    public AdminController(AdminService adminService, EventService eventService, BookingService bookingService,
                          AttendanceService attendanceService, UserService userService, NotificationService notificationService) {
        this.adminService = adminService;
        this.eventService = eventService;
        this.bookingService = bookingService;
        this.attendanceService = attendanceService;
        this.userService = userService;
        this.notificationService = notificationService;
    }
    
    /**
     * Create a new admin (Sign Up) - Secure version using DTOs
     * POST /api/admin
     * @param adminRequest The admin registration request DTO
     * @return Created admin response (without password)
     */
    @PostMapping
    public ResponseEntity<?> createAdmin(@Valid @RequestBody AdminRequestDTO adminRequest) {
        try {
            AdminResponseDTO createdAdmin = adminService.createAdmin(adminRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAdmin);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * Authenticate admin (Sign In)
     * POST /api/admin/authenticate
     * @param credentials The authentication credentials
     * @return Admin info if authentication successful
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateAdmin(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        
        try {
            Admin admin = adminService.authenticateAdmin(email, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", admin.getId());
            response.put("name", admin.getName());
            response.put("email", admin.getEmail());
            response.put("role", "admin");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    
    /**
     * Get admin by ID
     * GET /api/admin/{id}
     * @param id The admin ID
     * @return Admin
     */
    @GetMapping("/{id}")
    public ResponseEntity<Admin> getAdminById(@PathVariable Long id) {
        Admin admin = adminService.getAdminById(id);
        return ResponseEntity.ok(admin);
    }
    
    /**
     * Create a new event (Admin only)
     * POST /api/admin/events
     * Requires header: X-Admin-Id
     * @param eventDTO The event data
     * @return Created event DTO
     */
    @PostMapping("/events")
    public ResponseEntity<EventDTO> createEvent(@RequestHeader(value = "X-Admin-Id", required = false) Long adminId,
                                                @Valid @RequestBody EventDTO eventDTO) {
        EventDTO createdDTO = eventService.createEvent(eventDTO, adminId);
        Event eventEntity = eventService.getEventEntityById(createdDTO.getId());
        notificationService.sendEventAnnouncements(eventEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDTO);
    }
    
    /**
     * Update an existing event (Admin only)
     * PUT /api/admin/events/{id}
     * Requires header: X-Admin-Id
     * @param id The event ID
     * @param eventDTO The updated event data
     * @return Updated event DTO
     */
    @PutMapping("/events/{id}")
    public ResponseEntity<EventDTO> updateEvent(@PathVariable Long id, 
                                                @RequestHeader(value = "X-Admin-Id", required = false) Long adminId,
                                                @Valid @RequestBody EventDTO eventDTO) {
        EventDTO updatedEvent = eventService.updateEvent(id, eventDTO, adminId);
        return ResponseEntity.ok(updatedEvent);
    }
    
    /**
     * Delete an event (Admin only)
     * DELETE /api/admin/events/{id}
     * Requires header: X-Admin-Id
     * @param id The event ID
     * @return No content response
     */
    @DeleteMapping("/events/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        try {
            eventService.deleteEvent(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Database Error: " + cause);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Book an event as admin (Admin can make bookings like regular users)
     * POST /api/admin/events/{eventId}/book
     * @param eventId The event ID
     * @param bookingRequest The booking request data
     * @return Created booking response
     */
    @PostMapping("/events/{eventId}/book")
    public ResponseEntity<BookingResponseDTO> bookEvent(@PathVariable Long eventId,
                                                       @Valid @RequestBody BookingRequestDTO bookingRequest) {
        BookingResponseDTO booking = bookingService.createBooking(eventId, bookingRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }
    
    /**
     * Get all bookings (Admin view)
     * GET /api/admin/bookings
     * @return List of all bookings
     */
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        List<BookingResponseDTO> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get bookings by admin email
     * GET /api/admin/bookings/email/{email}
     * @param email The admin's email
     * @return List of bookings made by the admin
     */
    @GetMapping("/bookings/email/{email}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByEmail(@PathVariable String email) {
        List<BookingResponseDTO> bookings = bookingService.getBookingsByStudentEmail(email);
        return ResponseEntity.ok(bookings);
    }

    /**
     * QR check-in attendance list for an event
     * GET /api/admin/events/{eventId}/attendance
     */
    @GetMapping("/events/{eventId}/attendance")
    public ResponseEntity<List<AttendanceRowDTO>> getEventAttendance(@PathVariable Long eventId) {
        return ResponseEntity.ok(attendanceService.listAttendanceForEvent(eventId));
    }

    /**
     * Partially update a user (admin panel). Omits password from the body to leave it unchanged.
     */
    public record UserPatchRequest(String name, String email, String password) {
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<?> patchUser(@PathVariable Long id, @RequestBody UserPatchRequest body) {
        try {
            User updated = userService.patchUser(id, body.name(), body.email(), body.password());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}



