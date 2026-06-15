package com.badyauniversity.eventbooking.repository;

import com.badyauniversity.eventbooking.model.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventAttendanceRepository extends JpaRepository<EventAttendance, Long> {

    Optional<EventAttendance> findByEvent_IdAndUser_Id(Long eventId, Long userId);

    List<EventAttendance> findByEvent_IdOrderByCheckedInAtAsc(Long eventId);
}
