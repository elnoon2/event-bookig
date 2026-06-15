package com.badyauniversity.eventbooking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Event Entity representing university events
 */
@Entity
@Table(name = "events")
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(nullable = false)
    private String title;
    
    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 1000)
    private String description;
    
    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category; // academic, sports, cultural, social, workshop, seminar
    
    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;
    
    @NotNull(message = "Time is required")
    @Column(nullable = false)
    private LocalTime time;
    
    @NotBlank(message = "Venue is required")
    @Column(nullable = false)
    private String venue;
    
    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(nullable = false)
    private Integer capacity;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(length = 500)
    private String image;
    
    @NotBlank(message = "Organizer is required")
    @Column(nullable = false)
    private String organizer;
    
    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be valid")
    @Column(nullable = false)
    private String contactEmail;

    @Column(name = "target_faculties", length = 1000)
    private String targetFaculties;

    /**
     * Admin owner who created this event (used to restrict editing to the creator).
     * Nullable for legacy/sample events.
     */
    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;
    
    // Constructors
    public Event() {
    }
    
    public Event(String title, String description, String category, LocalDate date, 
                LocalTime time, String venue, Integer capacity, BigDecimal price, 
                String image, String organizer, String contactEmail) {
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
    
    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", date=" + date +
                ", venue='" + venue + '\'' +
                '}';
    }
}

