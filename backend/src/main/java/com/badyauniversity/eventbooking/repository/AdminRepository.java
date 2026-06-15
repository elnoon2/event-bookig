package com.badyauniversity.eventbooking.repository;

import com.badyauniversity.eventbooking.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Admin entity
 * Provides data access operations for admins
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    /**
     * Find admin by email
     * @param email The admin's email
     * @return Optional containing the admin if found
     */
    Optional<Admin> findByEmail(String email);
    
    /**
     * Check if an admin exists with the given email
     * @param email The email to check
     * @return true if admin exists, false otherwise
     */
    boolean existsByEmail(String email);
}



