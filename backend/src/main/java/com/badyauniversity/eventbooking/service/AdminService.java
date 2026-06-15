package com.badyauniversity.eventbooking.service;

import com.badyauniversity.eventbooking.dto.AdminRequestDTO;
import com.badyauniversity.eventbooking.dto.AdminResponseDTO;
import com.badyauniversity.eventbooking.model.Admin;
import com.badyauniversity.eventbooking.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service class for Admin business logic
 * Handles all admin-related operations
 */
@Service
@Transactional
public class AdminService {
    
    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String allowedAdminEmail;
    private final String allowedAdminPassword;
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    @Autowired
    public AdminService(AdminRepository adminRepository,
                        BCryptPasswordEncoder passwordEncoder,
                        @Value("${app.admin.email:admin@gmail.com}") String allowedAdminEmail,
                        @Value("${app.admin.password:0000}") String allowedAdminPassword) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.allowedAdminEmail = allowedAdminEmail.trim().toLowerCase();
        this.allowedAdminPassword = allowedAdminPassword;
    }
    
    /**
     * Create a new admin from DTO
     * @param adminRequest The admin request DTO
     * @return Created admin response DTO
     * @throws RuntimeException if email already exists or validation fails
     */
    public AdminResponseDTO createAdmin(AdminRequestDTO adminRequest) {
        // Validate and normalize email
        String email = adminRequest.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        email = email.trim().toLowerCase();
        
        // Additional email validation
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Invalid email format");
        }


        
        // Check if email already exists
        if (adminRepository.existsByEmail(email)) {
            throw new RuntimeException("An admin with this email already exists");
        }
        
        // Validate and normalize name
        String name = adminRequest.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name is required");
        }
        name = name.trim();
        
        // Validate password
        String password = adminRequest.getPassword();
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Password is required");
        }


        
        // Create admin entity
        Admin admin = new Admin();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        
        // Save admin
        Admin savedAdmin = adminRepository.save(admin);
        
        // Convert to response DTO (excludes password)
        return convertToResponseDTO(savedAdmin);
    }
    
    /**
     * Validate password strength
     * @param password The password to validate
     * @throws RuntimeException if password doesn't meet strength requirements
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }
        
        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpperCase = true;
            else if (Character.isLowerCase(c)) hasLowerCase = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("@$!%*?&".indexOf(c) >= 0) hasSpecialChar = true;
        }
        
        if (!hasUpperCase) {
            throw new RuntimeException("Password must contain at least one uppercase letter");
        }
        if (!hasLowerCase) {
            throw new RuntimeException("Password must contain at least one lowercase letter");
        }
        if (!hasDigit) {
            throw new RuntimeException("Password must contain at least one number");
        }
        if (!hasSpecialChar) {
            throw new RuntimeException("Password must contain at least one special character (@$!%*?&)");
        }
    }
    
    /**
     * Convert Admin entity to AdminResponseDTO
     * @param admin The admin entity
     * @return AdminResponseDTO
     */
    private AdminResponseDTO convertToResponseDTO(Admin admin) {
        AdminResponseDTO dto = new AdminResponseDTO();
        dto.setId(admin.getId());
        dto.setName(admin.getName());
        dto.setEmail(admin.getEmail());
        dto.setCreatedAt(admin.getCreatedAt());
        dto.setUpdatedAt(admin.getUpdatedAt());
        return dto;
    }
    
    /**
     * Create a new admin (legacy method for backward compatibility)
     * @param admin The admin entity
     * @return Created admin
     * @throws RuntimeException if email already exists
     */
    public Admin createAdmin(Admin admin) {
        if (adminRepository.existsByEmail(admin.getEmail())) {
            throw new RuntimeException("Admin with email " + admin.getEmail() + " already exists");
        }
        // Hash the password before saving
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        return adminRepository.save(admin);
    }
    
    /**
     * Get admin by ID
     * @param id The admin ID
     * @return Admin if found
     * @throws RuntimeException if admin not found
     */
    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + id));
    }
    
    /**
     * Get admin by email
     * @param email The admin's email
     * @return Admin if found
     */
    public Optional<Admin> getAdminByEmail(String email) {
        return adminRepository.findByEmail(email);
    }
    
    /**
     * Get all admins
     * @return List of all admins
     */
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }
    
    /**
     * Update admin
     * @param id The admin ID
     * @param admin The updated admin data
     * @return Updated admin
     * @throws RuntimeException if admin not found
     */
    public Admin updateAdmin(Long id, Admin admin) {
        Admin existingAdmin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + id));
        
        existingAdmin.setName(admin.getName());
        existingAdmin.setEmail(admin.getEmail());
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            // Hash the password before updating
            existingAdmin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        
        return adminRepository.save(existingAdmin);
    }
    
    /**
     * Delete admin
     * @param id The admin ID
     * @throws RuntimeException if admin not found
     */
    public void deleteAdmin(Long id) {
        if (!adminRepository.existsById(id)) {
            throw new RuntimeException("Admin not found with id: " + id);
        }
        adminRepository.deleteById(id);
    }
    
    /**
     * Authenticate admin
     * @param email The admin's email
     * @param password The admin's password
     * @return Admin if authentication successful
     * @throws RuntimeException if authentication fails
     */
    public Admin authenticateAdmin(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();


        Admin admin = adminRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        String stored = admin.getPassword() == null ? "" : admin.getPassword();
        boolean ok;
        // Support both BCrypt-hashed passwords and legacy plaintext seeds.
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            ok = passwordEncoder.matches(password, stored);
        } else {
            ok = stored.equals(password);
        }
        if (!ok) throw new RuntimeException("Invalid email or password");

        return admin;
    }
}



