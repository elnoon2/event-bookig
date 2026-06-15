package com.badyauniversity.eventbooking.repository;

import com.badyauniversity.eventbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 * Provides data access operations for users
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email
     * @param email The user's email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a user exists with the given email
     * @param email The email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Find user by username, password, and role
     * Equivalent to: SELECT * FROM users WHERE username = :username AND password = :password AND role = :role
     * @param username The username
     * @param password The password
     * @param role The role (e.g., 'admin')
     * @return Optional containing the user if found
     */
    Optional<User> findByUsernameAndPasswordAndRole(String username, String password, String role);

    Optional<User> findByQrToken(String qrToken);

    java.util.List<User> findByQrTokenIsNull();

    Optional<User> findByAttendanceToken(String token);

    /**
     * Find a user previously provisioned through an OAuth provider (e.g. Microsoft).
     * @param oauthProvider The provider key, e.g. "microsoft"
     * @param oauthId The stable provider-specific user id (Microsoft Entra "oid")
     * @return Optional containing the user if found
     */
    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}

