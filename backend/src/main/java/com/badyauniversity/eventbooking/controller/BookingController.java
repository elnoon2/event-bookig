package com.badyauniversity.eventbooking.controller;

import com.badyauniversity.eventbooking.dto.BookingRequestDTO;
import com.badyauniversity.eventbooking.dto.BookingResponseDTO;
import com.badyauniversity.eventbooking.service.BookingService;
import com.badyauniversity.eventbooking.service.QrCodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Booking operations
 * Handles HTTP requests related to bookings
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class BookingController {
    
    private final BookingService bookingService;
    private final QrCodeService qrCodeService;
    
    @Autowired
    public BookingController(BookingService bookingService, QrCodeService qrCodeService) {
        this.bookingService = bookingService;
        this.qrCodeService = qrCodeService;
    }
    
    /**
     * Book an event
     * POST /api/events/{eventId}/book
     * @param eventId The event ID
     * @param bookingRequest The booking request data
     * @return Created booking response
     */
    @PostMapping("/{eventId}/book")
    public ResponseEntity<BookingResponseDTO> bookEvent(@PathVariable Long eventId,
                                                        @Valid @RequestBody BookingRequestDTO bookingRequest) {
        BookingResponseDTO booking = bookingService.createBooking(eventId, bookingRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }
    
    /**
     * Get all bookings
     * GET /api/bookings
     * @return List of all bookings
     */
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        List<BookingResponseDTO> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get booking by ID
     * GET /api/bookings/{id}
     * @param id The booking ID
     * @return Booking response
     */
    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        BookingResponseDTO booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * Get bookings by student email
     * GET /api/bookings/student/{email}
     * @param email The student's email
     * @return List of bookings made by the student
     */
    @GetMapping("/bookings/student/{email}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByStudentEmail(@PathVariable String email) {
        List<BookingResponseDTO> bookings = bookingService.getBookingsByStudentEmail(email);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get bookings by event ID
     * GET /api/events/{eventId}/bookings
     * @param eventId The event ID
     * @return List of bookings for the event
     */
    @GetMapping("/{eventId}/bookings")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByEventId(@PathVariable Long eventId) {
        List<BookingResponseDTO> bookings = bookingService.getBookingsByEventId(eventId);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get available capacity for an event
     * GET /api/events/{eventId}/capacity
     * @param eventId The event ID
     * @return Available capacity
     */
    @GetMapping("/{eventId}/capacity")
    public ResponseEntity<Integer> getAvailableCapacity(@PathVariable Long eventId) {
        Integer capacity = bookingService.getAvailableCapacity(eventId);
        return ResponseEntity.ok(capacity);
    }
    
    /**
     * Delete a booking
     * DELETE /api/bookings/{id}
     * @param id The booking ID
     * @return No content response
     */
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get booking ticket QR image
     * GET /api/events/bookings/{id}/qr
     */
    @GetMapping(value = "/bookings/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBookingTicketQr(@PathVariable Long id) {
        BookingResponseDTO booking = bookingService.getBookingById(id);
        if (booking.getQrToken() == null) {
            throw new RuntimeException("QR Token has not been generated for this booking.");
        }
        byte[] pngBytes = qrCodeService.encodeToPngBytes(booking.getQrToken(), 300);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(pngBytes);
    }

    /**
     * Scan ticket QR
     * POST /api/events/bookings/scan
     */
    @PostMapping("/bookings/scan")
    public ResponseEntity<BookingResponseDTO> scanTicket(@RequestBody Map<String, Object> payload) {
        String qrToken = (String) payload.get("qrToken");
        Long eventId = null;
        if (payload.get("eventId") != null) {
            eventId = Long.valueOf(payload.get("eventId").toString());
        }
        String adminUsername = (String) payload.get("adminUsername");
        BookingResponseDTO scanned = bookingService.scanTicket(qrToken, eventId, adminUsername);
        return ResponseEntity.ok(scanned);
    }
}

