package com.badyauniversity.eventbooking.dto;

import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for Booking Request
 * Used when creating a new booking
 */
public class BookingRequestDTO {
    
    @NotBlank(message = "Student name is required")
    @Size(max = 100, message = "Student name must not exceed 100 characters")
    private String studentName;
    
    @NotBlank(message = "Student ID is required")
    @Size(max = 50, message = "Student ID must not exceed 50 characters")
    private String studentId;
    
    @NotBlank(message = "Student email is required")
    @Email(message = "Student email must be valid")
    private String studentEmail;
    
    @NotNull(message = "Number of tickets is required")
    @Min(value = 1, message = "At least 1 ticket must be booked")
    private Integer tickets;

    /** Required for paid events: VISA or CASH (case-insensitive). Omit or null for free events. */
    private String paymentMethod;
    
    // Constructors
    public BookingRequestDTO() {
    }
    
    public BookingRequestDTO(String studentName, String studentId, String studentEmail, Integer tickets) {
        this.studentName = studentName;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.tickets = tickets;
    }
    
    // Getters and Setters
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
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

