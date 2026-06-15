package com.badyauniversity.eventbooking.repository;

import com.badyauniversity.eventbooking.model.Booking;
import com.badyauniversity.eventbooking.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Booking entity
 * Provides data access operations for bookings
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    /**
     * Find all bookings for a specific event
     * @param event The event
     * @return List of bookings for the event
     */
    List<Booking> findByEvent(Event event);
    
    /**
     * Find all bookings by student email
     * @param studentEmail The student's email
     * @return List of bookings made by the student
     */
    List<Booking> findByStudentEmail(String studentEmail);
    
    /**
     * Find all bookings by student ID
     * @param studentId The student's ID
     * @return List of bookings made by the student
     */
    List<Booking> findByStudentId(String studentId);
    
    /**
     * Count total tickets booked for an event
     * @param eventId The event ID
     * @return Total number of tickets booked
     */
    @Query("SELECT COALESCE(SUM(b.tickets), 0) FROM Booking b WHERE b.event.id = :eventId")
    Integer countTotalTicketsByEventId(@Param("eventId") Long eventId);
    
    /**
     * Find bookings by event ID
     * @param eventId The event ID
     * @return List of bookings for the event
     */
    List<Booking> findByEventId(Long eventId);

    boolean existsByEvent_IdAndStudentEmail(Long eventId, String studentEmail);

    java.util.Optional<Booking> findByQrToken(String qrToken);
}

