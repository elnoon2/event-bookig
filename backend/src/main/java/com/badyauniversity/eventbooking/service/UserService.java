package com.badyauniversity.eventbooking.service;

import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for User business logic
 * Handles all user-related operations
 */
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, 
                       QrCodeService qrCodeService,
                       org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Create a new user
     * @param user The user entity
     * @return Created user
     * @throws RuntimeException if email already exists
     */
    public User createUser(User user) {
        if (user.getEmail() == null || !user.getEmail().trim().toLowerCase().endsWith("@badyauni.edu.eg")) {
            throw new IllegalArgumentException("Only badyauni.edu.eg emails are allowed.");
        }
        if (user.getName() == null || user.getName().trim().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        String cleanName = user.getName().trim().replaceAll("\\s+", " ");
        String[] parts = cleanName.split(" ");
        if (parts.length < 3 || !cleanName.matches("^[\\p{L}\\s]+$")) {
            throw new IllegalArgumentException("Please enter your full triple name.");
        }
        user.setName(cleanName);

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("User with email " + user.getEmail() + " already exists");
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        assignQrCodeIfMissing(user);
        return userRepository.save(user);
    }

    /**
     * Ensures legacy rows (created before QR support) get a stable token and stored image.
     */
    public void ensureQrCodesForLegacyUsers() {
        for (User u : userRepository.findByQrTokenIsNull()) {
            assignQrCodeIfMissing(u);
            userRepository.save(u);
        }
    }

    private void assignQrCodeIfMissing(User user) {
        if (user.getQrToken() == null || user.getQrToken().isBlank()) {
            String token = qrCodeService.newStudentToken();
            user.setQrToken(token);
            user.setQrImageBase64(qrCodeService.encodeToPngDataUrl(token));
        }
    }
    
    /**
     * Get user by ID
     * @param id The user ID
     * @return User if found
     * @throws RuntimeException if user not found
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
    
    /**
     * Get user by email
     * @param email The user's email
     * @return User if found
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Get all users
     * @return List of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Update user
     * @param id The user ID
     * @param user The updated user data
     * @return Updated user
     * @throws RuntimeException if user not found
     */
    public User updateUser(Long id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        // Phone is immutable once set
        if (existingUser.getPhone() == null || existingUser.getPhone().isBlank()) {
            existingUser.setPhone(user.getPhone());
        }
        existingUser.setFaculty(user.getFaculty());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        assignQrCodeIfMissing(existingUser);
        
        return userRepository.save(existingUser);
    }

    /**
     * Partial update (admin): only non-null, non-blank fields are applied.
     */
    public User patchUser(Long id, String name, String email, String newPassword) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        if (name != null && !name.isBlank()) {
            existingUser.setName(name.trim());
        }
        if (email != null && !email.isBlank()) {
            existingUser.setEmail(email.trim().toLowerCase());
        }
        if (newPassword != null && !newPassword.isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(newPassword));
        }
        assignQrCodeIfMissing(existingUser);
        return userRepository.save(existingUser);
    }
    
    /**
     * Delete user
     * @param id The user ID
     * @throws RuntimeException if user not found
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    
    /**
     * Find an existing user for a Microsoft (OAuth) login, or create one automatically.
     *
     * Lookup order:
     *   1. Match on (provider, oauthId) — a returning Microsoft user.
     *   2. Match on email — links a pre-existing password account to Microsoft.
     *   3. Otherwise create a brand new account.
     *
     * New accounts get a strong random (un-guessable) BCrypt-hashed password so they satisfy the
     * NOT NULL / @Size password constraints while remaining unusable for password login, plus a
     * QR token via the existing {@link #assignQrCodeIfMissing} logic. This does NOT go through the
     * triple-name / @Valid signup path, since names returned by Microsoft are arbitrary.
     *
     * @param oauthId       Stable Microsoft Entra object id ("oid")
     * @param email         Verified email / preferred_username (already domain-checked by the caller)
     * @param fullName      Display name from the ID token / Graph
     * @param photoDataUrl  Profile photo as a data URL from Microsoft Graph, or null if unavailable
     * @param faculty       Microsoft Graph "department" (university faculty/major), or null
     * @param phone         Microsoft Graph mobile phone, or null
     * @param studentId     Microsoft Graph "employeeId" (university student number), or null
     * @return the existing or newly-created user
     */
    public User provisionMicrosoftUser(String oauthId, String email, String fullName,
                                       String photoDataUrl, String faculty, String phone, String studentId) {
        final String provider = "microsoft";
        final String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        final String safeName = (fullName == null || fullName.isBlank()) ? normalizedEmail : fullName.trim();

        // 1. Returning Microsoft user (matched by stable provider id).
        Optional<User> byOauth = userRepository.findByOauthProviderAndOauthId(provider, oauthId);
        if (byOauth.isPresent()) {
            User user = byOauth.get();
            if (photoDataUrl != null) {
                user.setProfilePhotoBase64(photoDataUrl);
            }
            fillProfileIfEmpty(user, faculty, phone, studentId);
            assignQrCodeIfMissing(user);
            return userRepository.save(user);
        }

        // 2. Existing local account with the same email -> link it to Microsoft.
        Optional<User> byEmail = userRepository.findByEmail(normalizedEmail);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setOauthProvider(provider);
            user.setOauthId(oauthId);
            if (photoDataUrl != null) {
                user.setProfilePhotoBase64(photoDataUrl);
            }
            fillProfileIfEmpty(user, faculty, phone, studentId);
            assignQrCodeIfMissing(user);
            return userRepository.save(user);
        }

        // 3. Brand new account.
        User user = new User();
        user.setName(safeName);
        user.setEmail(normalizedEmail);
        user.setRole("USER");
        user.setOauthProvider(provider);
        user.setOauthId(oauthId);
        user.setProfilePhotoBase64(photoDataUrl);
        fillProfileIfEmpty(user, faculty, phone, studentId);
        // Random, un-guessable placeholder password (BCrypt-hashed). The user logs in via Microsoft only.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID() + ":" + UUID.randomUUID()));
        assignQrCodeIfMissing(user);
        return userRepository.save(user);
    }

    /** Sets faculty/phone/studentId from Microsoft Graph only when the user hasn't already provided them. */
    private void fillProfileIfEmpty(User user, String faculty, String phone, String studentId) {
        if (faculty != null && !faculty.isBlank() && (user.getFaculty() == null || user.getFaculty().isBlank())) {
            user.setFaculty(faculty.trim());
        }
        if (phone != null && !phone.isBlank() && (user.getPhone() == null || user.getPhone().isBlank())) {
            user.setPhone(phone.trim());
        }
        if (studentId != null && !studentId.isBlank() && (user.getStudentId() == null || user.getStudentId().isBlank())) {
            user.setStudentId(studentId.trim());
        }
    }

    /**
     * Lets a signed-in user update their own editable profile fields.
     * PHONE IS IMMUTABLE after first save — if the user already has a phone number,
     * any attempt to change it is silently ignored.
     */
    public User updateSelfProfile(Long id, String phone, String faculty) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        // Phone is immutable once set: only allow setting if currently null/blank
        if (phone != null && (user.getPhone() == null || user.getPhone().isBlank())) {
            user.setPhone(phone.trim());
        } else if (phone != null && user.getPhone() != null && !user.getPhone().isBlank()) {
            throw new RuntimeException("WhatsApp number is already set and cannot be changed.");
        }
        if (faculty != null && !faculty.isBlank()) {
            user.setFaculty(faculty.trim());
        }
        return userRepository.save(user);
    }

    /**
     * One-time phone number save. Called when a user tries to book for the first time
     * and has no WhatsApp number stored yet. Rejects the call if the phone is already set.
     *
     * @param id    The user ID
     * @param phone The WhatsApp phone number to save
     * @return Updated user
     * @throws RuntimeException if the phone is already set or validation fails
     */
    public User setPhoneOnce(Long id, String phone) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            throw new RuntimeException("WhatsApp number is already set and cannot be changed.");
        }
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Phone number is required.");
        }
        String cleaned = phone.trim();
        if (!cleaned.matches("^\\+?\\d{6,15}$")) {
            throw new RuntimeException("Invalid phone number format. Use digits only, e.g. 201001234567.");
        }
        user.setPhone(cleaned);
        return userRepository.save(user);
    }

    /**
     * Authenticate user
     * @param email The user's email
     * @param password The user's password
     * @return User if authentication successful
     * @throws RuntimeException if authentication fails
     */
    public User authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        String stored = user.getPassword() == null ? "" : user.getPassword();
        boolean ok;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            ok = passwordEncoder.matches(password, stored);
        } else {
            ok = stored.equals(password); // Support legacy plaintext
        }

        if (!ok) {
            throw new RuntimeException("Invalid email or password");
        }
        
        return user;
    }
}

