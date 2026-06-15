package com.badyauniversity.eventbooking.controller;

import com.badyauniversity.eventbooking.dto.AttendanceCheckInRequestDTO;
import com.badyauniversity.eventbooking.dto.AttendanceCheckInResponseDTO;
import com.badyauniversity.eventbooking.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * QR-based event attendance check-in for students.
 */
@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Autowired
    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * POST /api/attendance/check-in
     * Body: { "eventId": 1, "qrToken": "<value read from QR>" }
     */
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceCheckInResponseDTO> checkIn(@Valid @RequestBody AttendanceCheckInRequestDTO body) {
        AttendanceCheckInResponseDTO result = attendanceService.checkIn(body.getEventId(), body.getQrToken());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/attendance/scan?token=XYZ
     * Body: { "eventId": 1 }
     */
    public record AttendanceScanRequest(Long eventId) {
    }

    @PostMapping("/scan")
    public ResponseEntity<AttendanceCheckInResponseDTO> scan(@RequestParam("token") String token,
                                                             @RequestBody(required = false) AttendanceScanRequest body,
                                                             @RequestParam(value = "eventId", required = false) Long eventId) {
        Long resolvedEventId = eventId != null ? eventId : (body != null ? body.eventId() : null);
        AttendanceCheckInResponseDTO result = attendanceService.scan(resolvedEventId, token);
        return ResponseEntity.ok(result);
    }
}
