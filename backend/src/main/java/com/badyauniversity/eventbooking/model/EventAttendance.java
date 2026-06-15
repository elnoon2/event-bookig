package com.badyauniversity.eventbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records that a student (user) attended an event, registered via QR check-in.
 */
@Entity
@Table(name = "event_attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_event_attendance_event_user", columnNames = {"event_id", "user_id"}))
public class EventAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    public EventAttendance() {
    }

    public EventAttendance(Event event, User user, LocalDateTime checkedInAt) {
        this.event = event;
        this.user = user;
        this.checkedInAt = checkedInAt;
    }

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    @PrePersist
    protected void onCreate() {
        if (checkedInAt == null) {
            checkedInAt = LocalDateTime.now();
        }
    }
}
