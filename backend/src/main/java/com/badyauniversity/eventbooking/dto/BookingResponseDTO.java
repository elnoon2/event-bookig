package com.badyauniversity.eventbooking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Booking Response
 * Used when returning booking information
 */
public class BookingResponseDTO {
    
    private Long id;
    private Long eventId;
    private EventDTO event;
    private String studentName;
    private String studentId;
    private String studentEmail;
    private Integer tickets;
    private BigDecimal totalAmount;
    private LocalDateTime bookingDate;
    private String paymentMethod;
    private String qrToken;
    private String ticketStatus;
    private LocalDateTime scannedAt;
    private String scannedBy;
    
    // Constructors
    public BookingResponseDTO() {
    }
    
    public BookingResponseDTO(Long id, Long eventId, EventDTO event, String studentName, 
                             String studentId, String studentEmail, Integer tickets, 
                             BigDecimal totalAmount, LocalDateTime bookingDate) {
        this.id = id;
        this.eventId = eventId;
        this.event = event;
        this.studentName = studentName;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.tickets = tickets;
        this.totalAmount = totalAmount;
        this.bookingDate = bookingDate;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getEventId() {
        return eventId;
    }
    
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
    
    public EventDTO getEvent() {
        return event;
    }
    
    public void setEvent(EventDTO event) {
        this.event = event;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getStudentEmail() {
        return studentEmail;
    }
    
    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }
    
    public Integer getTickets() {
        return tickets;
    }
    
    public void setTickets(Integer tickets) {
        this.tickets = tickets;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public LocalDateTime getBookingDate() {
        return bookingDate;
    }
    
    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(String ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }

    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }
}

