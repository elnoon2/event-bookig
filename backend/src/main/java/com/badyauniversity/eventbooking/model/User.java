package com.badyauniversity.eventbooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * User Entity representing system users (students/faculty)
 */
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false)
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(unique = true)
    private String username;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password is required")
    @Size(min = 4, message = "Password must be at least 4 characters")
    @Column(nullable = false)
    private String password;
    
    @Column(length = 20)
    private String role;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 100)
    private String faculty;
    
    @Column(name = "student_id", length = 50)
    private String studentId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Unique immutable token encoded in the student's QR code (identifies the student). */
    @Column(name = "qr_token", unique = true, length = 64)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String qrToken;

    /** Stored PNG image as a data URL (data:image/png;base64,...) for profile display. */
    @Lob
    @Column(name = "qr_image_base64", columnDefinition = "LONGTEXT")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String qrImageBase64;

    // --- File-based QR system (saved under uploads/qr/) ---
    @Column(name = "profile_qr_path")
    private String profileQrPath;

    @Column(name = "attendance_qr_path")
    private String attendanceQrPath;

    @Column(name = "attendance_token")
    @JsonIgnore
    private String attendanceToken;

    @Column(name = "token_expiry")
    @JsonIgnore
    private LocalDateTime tokenExpiry;

    /** OAuth provider for accounts created via social login (e.g. "microsoft"); null for password accounts. */
    @Column(name = "oauth_provider", length = 20)
    private String oauthProvider;

    /** Stable provider-specific user id (Microsoft Entra "oid"); null for password accounts. */
    @Column(name = "oauth_id", length = 255)
    @JsonIgnore
    private String oauthId;

    /** Profile photo from Microsoft Graph, stored as a data URL (data:image/jpeg;base64,...); may be null. */
    @Lob
    @Column(name = "profile_photo_base64", columnDefinition = "LONGTEXT")
    private String profilePhotoBase64;

    // Constructors
    public User() {
    }
    
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }
    
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getQrImageBase64() {
        return qrImageBase64;
    }

    public void setQrImageBase64(String qrImageBase64) {
        this.qrImageBase64 = qrImageBase64;
    }

    public String getProfileQrPath() {
        return profileQrPath;
    }

    public void setProfileQrPath(String profileQrPath) {
        this.profileQrPath = profileQrPath;
    }

    public String getAttendanceQrPath() {
        return attendanceQrPath;
    }

    public void setAttendanceQrPath(String attendanceQrPath) {
        this.attendanceQrPath = attendanceQrPath;
    }

    public String getAttendanceToken() {
        return attendanceToken;
    }

    public void setAttendanceToken(String attendanceToken) {
        this.attendanceToken = attendanceToken;
    }

    public LocalDateTime getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(LocalDateTime tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public String getOauthId() {
        return oauthId;
    }

    public void setOauthId(String oauthId) {
        this.oauthId = oauthId;
    }

    public String getProfilePhotoBase64() {
        return profilePhotoBase64;
    }

    public void setProfilePhotoBase64(String profilePhotoBase64) {
        this.profilePhotoBase64 = profilePhotoBase64;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

