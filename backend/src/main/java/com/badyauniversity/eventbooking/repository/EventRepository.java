package com.badyauniversity.eventbooking.repository;

import com.badyauniversity.eventbooking.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Event entity
 * Provides data access operations for events
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    /**
     * Find all events by category
     * @param category The event category
     * @return List of events in the specified category
     */
    List<Event> findByCategory(String category);
    
    /**
     * Find all events after a specific date
     * @param date The date to filter from
     * @return List of upcoming events
     */
    List<Event> findByDateAfter(LocalDate date);
    
    /**
     * Find events by category and date
     * @param category The event category
     * @param date The date to filter from
     * @return List of events matching the criteria
     */
    List<Event> findByCategoryAndDateAfter(String category, LocalDate date);
    
    /**
     * Find events by organizer
     * @param organizer The organizer name
     * @return List of events organized by the specified organizer
     */
    List<Event> findByOrganizer(String organizer);
    
    /**
     * Search events by title containing the search term
     * @param title The search term
     * @return List of events matching the search term
     */
    @Query("SELECT e FROM Event e WHERE e.title LIKE %:title%")
    List<Event> searchByTitle(@Param("title") String title);
}

