package com.badyauniversity.eventbooking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Booking Entity representing event bookings made by students
 */
@Entity
@Table(name = "bookings")
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Event is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @NotBlank(message = "Student name is required")
    @Size(max = 100, message = "Student name must not exceed 100 characters")
    @Column(nullable = false)
    private String studentName;
    
    @NotBlank(message = "Student ID is required")
    @Size(max = 50, message = "Student ID must not exceed 50 characters")
    @Column(nullable = false)
    private String studentId;
    
    @NotBlank(message = "Student email is required")
    @Email(message = "Student email must be valid")
    @Column(nullable = false)
    private String studentEmail;
    
    @NotNull(message = "Number of tickets is required")
    @Min(value = 1, message = "At least 1 ticket must be booked")
    @Column(nullable = false)
    private Integer tickets;
    
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount must be non-negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @NotNull(message = "Booking date is required")
    @Column(nullable = false)
    private LocalDateTime bookingDate;

    /** VISA or CASH for paid events; null when event is free. */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "qr_token", unique = true, length = 100)
    private String qrToken;

    @Column(name = "ticket_status", nullable = false, length = 20)
    private String ticketStatus = "ACTIVE";

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Column(name = "scanned_by", length = 100)
    private String scannedBy;
    
    // Constructors
    public Booking() {
        this.bookingDate = LocalDateTime.now();
        this.ticketStatus = "ACTIVE";
    }
    
    public Booking(Event event, String studentName, String studentId, 
                  String studentEmail, Integer tickets, BigDecimal totalAmount) {
        this.event = event;
        this.studentName = studentName;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.tickets = tickets;
        this.totalAmount = totalAmount;
        this.bookingDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Event getEvent() {
        return event;
    }
    
    public void setEvent(Event event) {
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
    
    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
        if (this.ticketStatus == null) {
            this.ticketStatus = "ACTIVE";
        }
    }
    
    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", event=" + (event != null ? event.getTitle() : "null") +
                ", studentName='" + studentName + '\'' +
                ", studentEmail='" + studentEmail + '\'' +
                ", tickets=" + tickets +
                ", totalAmount=" + totalAmount +
                ", bookingDate=" + bookingDate +
                '}';
    }
}

