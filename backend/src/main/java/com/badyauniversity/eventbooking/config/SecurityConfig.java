package com.badyauniversity.eventbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Security Configuration
 * Provides security-related beans for the application
 */
@Configuration
public class SecurityConfig {
    
    /**
     * BCryptPasswordEncoder bean for password hashing
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

