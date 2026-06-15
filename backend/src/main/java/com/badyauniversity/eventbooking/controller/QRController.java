package com.badyauniversity.eventbooking.controller;

import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.repository.UserRepository;
import com.badyauniversity.eventbooking.service.QrCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class QRController {

    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final String siteBaseUrl;

    public QRController(UserRepository userRepository,
                        QrCodeService qrCodeService,
                        @Value("${app.site.base-url:}") String siteBaseUrl) {
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.siteBaseUrl = siteBaseUrl == null ? "" : siteBaseUrl.trim().replaceAll("/+$", "");
    }

    private String toAbsoluteOrRelative(String path) {
        if (siteBaseUrl.isBlank()) return path;
        if (!path.startsWith("/")) path = "/" + path;
        return siteBaseUrl + path;
    }

    /**
     * GET /user/{id}/generate-profile-qr
     * Generates QR with URL: /user/{id}
     * Saves image as profile_{id}.png, stores path in user.profileQrPath.
     */
    @GetMapping("/{id}/generate-profile-qr")
    public ResponseEntity<?> generateProfileQr(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fileName = "profile_" + id + ".png";
        String url = toAbsoluteOrRelative("/user/" + id);
        String publicPath = qrCodeService.generateAndSaveQRCode(url, fileName);

        user.setProfileQrPath(publicPath);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "profileQrPath", user.getProfileQrPath()
        ));
    }

    /**
     * GET /user/{id}/generate-attendance-qr
     * Generates UUID token, saves token+expiry (5 minutes),
     * generates QR with URL: /attendance/scan?token=XYZ
     * saves image as attendance_{id}.png, stores path in user.attendanceQrPath.
     */
    @GetMapping("/{id}/generate-attendance-qr")
    public ResponseEntity<?> generateAttendanceQr(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();
        user.setAttendanceToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(5));

        String fileName = "attendance_" + id + ".png";
        String url = toAbsoluteOrRelative("/attendance/scan?token=" + token);
        String publicPath = qrCodeService.generateAndSaveQRCode(url, fileName);

        user.setAttendanceQrPath(publicPath);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "attendanceQrPath", user.getAttendanceQrPath(),
                "tokenExpiry", user.getTokenExpiry().toString()
        ));
    }
}

