package com.badyauniversity.eventbooking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data Transfer Object for Event
 * Used for transferring event data between layers
 */
public class EventDTO {
    
    @JsonProperty("_id")
    private Long id;
    private String title;
    private String description;
    private String category;
    private LocalDate date;
    private LocalTime time;
    private String venue;
    private Integer capacity;
    /** Tickets still available (capacity minus sum of booked tickets). */
    private Integer ticketsAvailable;
    private BigDecimal price;
    private String image;
    private String organizer;
    private String contactEmail;
    
    private String targetFaculties;

    /** Admin owner who created the event (used to enforce "edit your own events"). */
    private Long createdByAdminId;
    
    // Constructors
    public EventDTO() {
    }
    
    public EventDTO(Long id, String title, String description, String category, 
                   LocalDate date, LocalTime time, String venue, Integer capacity, 
                   BigDecimal price, String image, String organizer, String contactEmail) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.date = date;
        this.time = time;
        this.venue = venue;
        this.capacity = capacity;
        this.price = price;
        this.image = image;
        this.organizer = organizer;
        this.contactEmail = contactEmail;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public LocalTime getTime() {
        return time;
    }
    
    public void setTime(LocalTime time) {
        this.time = time;
    }
    
    public String getVenue() {
        return venue;
    }
    
    public void setVenue(String venue) {
        this.venue = venue;
    }
    
    public Integer getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
    
    public Integer getTicketsAvailable() {
        return ticketsAvailable;
    }
    
    public void setTicketsAvailable(Integer ticketsAvailable) {
        this.ticketsAvailable = ticketsAvailable;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public String getOrganizer() {
        return organizer;
    }
    
    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getTargetFaculties() {
        return targetFaculties;
    }

    public void setTargetFaculties(String targetFaculties) {
        this.targetFaculties = targetFaculties;
    }

    public Long getCreatedByAdminId() {
        return createdByAdminId;
    }

    public void setCreatedByAdminId(Long createdByAdminId) {
        this.createdByAdminId = createdByAdminId;
    }
}

