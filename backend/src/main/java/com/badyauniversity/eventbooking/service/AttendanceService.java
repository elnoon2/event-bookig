package com.badyauniversity.eventbooking.service;

import com.badyauniversity.eventbooking.dto.AttendanceCheckInResponseDTO;
import com.badyauniversity.eventbooking.dto.AttendanceRowDTO;
import com.badyauniversity.eventbooking.model.Event;
import com.badyauniversity.eventbooking.model.EventAttendance;
import com.badyauniversity.eventbooking.model.AttendanceToken;
import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.repository.BookingRepository;
import com.badyauniversity.eventbooking.repository.EventAttendanceRepository;
import com.badyauniversity.eventbooking.repository.EventRepository;
import com.badyauniversity.eventbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final AttendanceTokenService attendanceTokenService;

    @Autowired
    public AttendanceService(UserRepository userRepository,
                             EventRepository eventRepository,
                             BookingRepository bookingRepository,
                             EventAttendanceRepository eventAttendanceRepository,
                             AttendanceTokenService attendanceTokenService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
        this.eventAttendanceRepository = eventAttendanceRepository;
        this.attendanceTokenService = attendanceTokenService;
    }

    /**
     * Registers attendance for the current event when a student's QR token is scanned.
     * Student must have a booking for that event (matched by account email).
     */
    public AttendanceCheckInResponseDTO checkIn(Long eventId, String qrToken) {
        if (qrToken == null || qrToken.isBlank()) {
            throw new RuntimeException("Invalid QR code");
        }
        String token = qrToken.trim();

        User user = userRepository.findByQrToken(token)
                .orElseThrow(() -> new RuntimeException("Unknown student QR code"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        boolean hasBooking = bookingRepository.existsByEvent_IdAndStudentEmail(eventId, user.getEmail());
        if (!hasBooking) {
            throw new RuntimeException("This student has no booking for this event");
        }

        Optional<EventAttendance> existing = eventAttendanceRepository.findByEvent_IdAndUser_Id(eventId, user.getId());
        if (existing.isPresent()) {
            AttendanceCheckInResponseDTO dto = new AttendanceCheckInResponseDTO();
            dto.setMessage("Student is already registered for this event");
            dto.setAlreadyRegistered(true);
            dto.setStudentName(user.getName());
            dto.setStudentEmail(user.getEmail());
            dto.setEventId(event.getId());
            dto.setEventTitle(event.getTitle());
            dto.setCheckedInAt(existing.get().getCheckedInAt());
            return dto;
        }

        EventAttendance row = new EventAttendance();
        row.setEvent(event);
        row.setUser(user);
        row.setCheckedInAt(LocalDateTime.now());
        EventAttendance saved = eventAttendanceRepository.save(row);

        AttendanceCheckInResponseDTO dto = new AttendanceCheckInResponseDTO();
        dto.setMessage("Attendance registered successfully");
        dto.setAlreadyRegistered(false);
        dto.setStudentName(user.getName());
        dto.setStudentEmail(user.getEmail());
        dto.setEventId(event.getId());
        dto.setEventTitle(event.getTitle());
        dto.setCheckedInAt(saved.getCheckedInAt());
        return dto;
    }

    /**
     * New flow: consumes a temporary secure token issued for a user and registers attendance.
     * Token is hashed in DB, expires, and is one-time use.
     */
    public AttendanceCheckInResponseDTO scan(Long eventId, String token) {
        if (eventId == null) {
            throw new RuntimeException("eventId is required");
        }

        AttendanceToken consumed = attendanceTokenService.consume(token);
        User user = consumed.getUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        boolean hasBooking = bookingRepository.existsByEvent_IdAndStudentEmail(eventId, user.getEmail());
        if (!hasBooking) {
            throw new RuntimeException("This student has no booking for this event");
        }

        Optional<EventAttendance> existing = eventAttendanceRepository.findByEvent_IdAndUser_Id(eventId, user.getId());
        if (existing.isPresent()) {
            AttendanceCheckInResponseDTO dto = new AttendanceCheckInResponseDTO();
            dto.setMessage("Student is already registered for this event");
            dto.setAlreadyRegistered(true);
            dto.setStudentName(user.getName());
            dto.setStudentEmail(user.getEmail());
            dto.setEventId(event.getId());
            dto.setEventTitle(event.getTitle());
            dto.setCheckedInAt(existing.get().getCheckedInAt());
            return dto;
        }

        EventAttendance row = new EventAttendance();
        row.setEvent(event);
        row.setUser(user);
        row.setCheckedInAt(LocalDateTime.now());
        EventAttendance saved = eventAttendanceRepository.save(row);

        AttendanceCheckInResponseDTO dto = new AttendanceCheckInResponseDTO();
        dto.setMessage("Attendance registered successfully");
        dto.setAlreadyRegistered(false);
        dto.setStudentName(user.getName());
        dto.setStudentEmail(user.getEmail());
        dto.setEventId(event.getId());
        dto.setEventTitle(event.getTitle());
        dto.setCheckedInAt(saved.getCheckedInAt());
        return dto;
    }

    public List<AttendanceRowDTO> listAttendanceForEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found");
        }
        return eventAttendanceRepository.findByEvent_IdOrderByCheckedInAtAsc(eventId).stream()
                .map(a -> {
                    AttendanceRowDTO row = new AttendanceRowDTO();
                    row.setStudentName(a.getUser().getName());
                    row.setStudentEmail(a.getUser().getEmail());
                    row.setCheckedInAt(a.getCheckedInAt());
                    return row;
                })
                .collect(Collectors.toList());
    }
}
