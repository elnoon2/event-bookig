package com.badyauniversity.eventbooking.controller;

import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.service.AttendanceTokenService;
import com.badyauniversity.eventbooking.service.QrCodeService;
import com.badyauniversity.eventbooking.service.UserService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for User operations
 * Handles HTTP requests related to users
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    private final QrCodeService qrCodeService;
    private final AttendanceTokenService attendanceTokenService;
    
    @Autowired
    public UserController(UserService userService, QrCodeService qrCodeService, AttendanceTokenService attendanceTokenService) {
        this.userService = userService;
        this.qrCodeService = qrCodeService;
        this.attendanceTokenService = attendanceTokenService;
    }
    
    /**
     * Self-service registration is DISABLED — accounts are provisioned automatically through
     * "Sign in with Microsoft" (the only public login method). Returns 410 Gone.
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody(required = false) Map<String, Object> ignored) {
        return microsoftOnlyResponse();
    }

    /**
     * Email/password sign-in is DISABLED — use "Sign in with Microsoft" instead. Returns 410 Gone.
     * POST /api/users/authenticate
     */
    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, Object>> authenticateUser(@RequestBody(required = false) Map<String, String> ignored) {
        return microsoftOnlyResponse();
    }

    /** Shared 410 response directing users to Microsoft sign-in. */
    private ResponseEntity<Map<String, Object>> microsoftOnlyResponse() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.GONE.value());
        body.put("message", "Email/password login is disabled. Please sign in with Microsoft.");
        return ResponseEntity.status(HttpStatus.GONE).body(body);
    }
    
    /** Body for a user updating their own editable profile fields. */
    public record SelfProfileUpdate(String phone, String faculty) {
    }

    /**
     * Update the signed-in user's own editable fields (phone, faculty).
     * PATCH /api/users/{id}/profile
     */
    @PatchMapping("/{id}/profile")
    public ResponseEntity<Map<String, Object>> updateOwnProfile(@PathVariable Long id,
                                                                @RequestBody SelfProfileUpdate body) {
        User user = userService.updateSelfProfile(id, body.phone(), body.faculty());
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("phone", user.getPhone());
        response.put("faculty", user.getFaculty());
        response.put("studentId", user.getStudentId());
        return ResponseEntity.ok(response);
    }

    /** Body for the one-time phone number save. */
    public record PhoneOnceRequest(String phone) {
    }

    /**
     * One-time WhatsApp phone number save.
     * PUT /api/users/{id}/phone
     * Returns 400 if the phone is already set.
     */
    @PutMapping("/{id}/phone")
    public ResponseEntity<Map<String, Object>> setPhoneOnce(@PathVariable Long id,
                                                             @RequestBody PhoneOnceRequest body) {
        User user = userService.setPhoneOnce(id, body.phone());
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("phone", user.getPhone());
        response.put("phoneSet", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Check whether a user has a WhatsApp phone number stored.
     * GET /api/users/{id}/has-phone
     */
    @GetMapping("/{id}/has-phone")
    public ResponseEntity<Map<String, Object>> hasPhone(@PathVariable Long id) {
        User user = userService.getUserById(id);
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isBlank();
        Map<String, Object> response = new HashMap<>();
        response.put("hasPhone", hasPhone);
        response.put("phone", hasPhone ? user.getPhone() : null);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID
     * GET /api/users/{id}
     * @param id The user ID
     * @return User
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Static profile QR: encodes a stable URL like /user/{id}.
     * GET /api/users/{id}/qr-profile
     */
    @GetMapping(value = "/{id}/qr-profile", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getProfileQr(@PathVariable Long id, HttpServletRequest request) {
        userService.getUserById(id); // validate existence
        String baseUrl = getBaseUrl(request);
        String payload = baseUrl + "/profile.html?userId=" + id;
        byte[] png = qrCodeService.encodeToPngBytes(payload, 240);

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.IMAGE_PNG).body(png);
    }

    /**
     * Temporary attendance QR: encodes /attendance/scan?token=XYZ (token is rotated when refreshed).
     * GET /api/users/{id}/qr-attendance
     */
    @GetMapping(value = "/{id}/qr-attendance", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getAttendanceQr(@PathVariable Long id, HttpServletRequest request) {
        User user = userService.getUserById(id);
        AttendanceTokenService.IssuedToken issued = attendanceTokenService.issueForUser(user);
        String baseUrl = getBaseUrl(request);
        String payload = baseUrl + "/attendance/scan?token=" + issued.rawToken();
        byte[] png = qrCodeService.encodeToPngBytes(payload, 240);

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.add("X-Token-Expires-At", issued.expiresAt().toString());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.IMAGE_PNG).body(png);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            scheme = forwardedProto;
        }
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            return scheme + "://" + forwardedHost;
        }
        
        String portStr = "";
        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            portStr = ":" + serverPort;
        }
        
        return scheme + "://" + serverName + portStr;
    }
    
    /**
     * Get current QR code status (ACTIVE, USED, EXPIRED) for a user.
     * GET /api/users/{id}/qr-status
     */
    @GetMapping("/{id}/qr-status")
    public ResponseEntity<Map<String, String>> getQrStatus(@PathVariable Long id) {
        userService.getUserById(id); // validate user existence
        String status = attendanceTokenService.getTokenStatus(id);
        return ResponseEntity.ok(Map.of("status", status));
    }
    
    /**
     * Get all users
     * GET /api/users
     * @return List of all users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * Update user
     * PUT /api/users/{id}
     * @param id The user ID
     * @param user The updated user data
     * @return Updated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Delete user
     * DELETE /api/users/{id}
     * @param id The user ID
     * @return No content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

