package com.badyauniversity.eventbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Application Class
 * Entry point for the Badya University Event Booking Backend
 */
@SpringBootApplication
@EnableAsync
public class EventBookingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(EventBookingApplication.class, args);
    }
}

