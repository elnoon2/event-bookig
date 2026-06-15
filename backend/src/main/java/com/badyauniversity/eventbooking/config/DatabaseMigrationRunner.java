package com.badyauniversity.eventbooking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("===============================================");
        System.out.println("Running Database Migration Runner...");
        try {
            // Drop existing foreign keys (ignoring errors if they don't exist)
            try { jdbcTemplate.execute("ALTER TABLE bookings DROP FOREIGN KEY fk_bookings_event"); } catch(Exception e){}
            try { jdbcTemplate.execute("ALTER TABLE event_attendance DROP FOREIGN KEY fk_attendance_event"); } catch(Exception e){}
            try { jdbcTemplate.execute("ALTER TABLE notifications DROP FOREIGN KEY fk_notif_event"); } catch(Exception e){}
            
            // Add cascading foreign keys
            jdbcTemplate.execute("ALTER TABLE bookings ADD CONSTRAINT fk_bookings_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE");
            jdbcTemplate.execute("ALTER TABLE event_attendance ADD CONSTRAINT fk_attendance_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE");
            jdbcTemplate.execute("ALTER TABLE notifications ADD CONSTRAINT fk_notif_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE");
            
            System.out.println("Database constraints successfully updated for cascading deletes.");
        } catch (Exception e) {
            System.out.println("Note: Database migrations already applied or error occurred: " + e.getMessage());
        }
        System.out.println("===============================================");
    }
}
