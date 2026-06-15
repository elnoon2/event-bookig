package com.badyauniversity.eventbooking.config;

import com.badyauniversity.eventbooking.model.Admin;
import com.badyauniversity.eventbooking.model.Event;
import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.repository.AdminRepository;
import com.badyauniversity.eventbooking.repository.EventRepository;
import com.badyauniversity.eventbooking.repository.UserRepository;
import com.badyauniversity.eventbooking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Data Initializer
 * Populates the database with sample events and default admin on application startup
 */
@Component
public class DataInitializer implements CommandLineRunner {
    
    private final EventRepository eventRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String bootstrapAdminName;
    private final String bootstrapAdminEmail;
    private final String bootstrapAdminPassword;
    
    @Autowired
    public DataInitializer(EventRepository eventRepository, AdminRepository adminRepository,
                          UserRepository userRepository,
                          UserService userService,
                          BCryptPasswordEncoder passwordEncoder,
                          @Value("${app.admin.name:Admin}") String bootstrapAdminName,
                          @Value("${app.admin.email:admin@gmail.com}") String bootstrapAdminEmail,
                          @Value("${app.admin.password:0000}") String bootstrapAdminPassword) {
        this.eventRepository = eventRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminName = bootstrapAdminName;
        this.bootstrapAdminEmail = bootstrapAdminEmail.trim().toLowerCase();
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }
    
    @Override
    public void run(String... args) throws Exception {
        warnIfInsecureAdminPassword();

        // Ensure bootstrap admin exists (by email)
        Admin bootstrapAdmin = ensureBootstrapAdmin();

        userService.ensureQrCodesForLegacyUsers();
        ensureUser1ProfileQrPath();
        
        // Only initialize events if database is empty
        if (eventRepository.count() == 0) {
            initializeSampleEvents(bootstrapAdmin);
        }
    }
    
    /**
     * Loudly warns operators when the application is running with the well-known insecure default
     * admin password. Set APP_ADMIN_PASSWORD in the environment to silence this and secure the
     * bootstrap admin account.
     */
    private void warnIfInsecureAdminPassword() {
        if ("0000".equals(bootstrapAdminPassword)) {
            System.err.println("============================================================");
            System.err.println("SECURITY WARNING: bootstrap admin is using the default password '0000'.");
            System.err.println("Set the APP_ADMIN_PASSWORD environment variable before deploying.");
            System.err.println("============================================================");
        }
    }

    /**
     * Initialize default admin account
     * Creates a default admin if no admins exist in the database
     */
    private Admin ensureBootstrapAdmin() {
        Optional<Admin> existing = adminRepository.findByEmail(bootstrapAdminEmail);
        if (existing.isPresent()) {
            Admin admin = existing.get();
            admin.setName(bootstrapAdminName);
            admin.setPassword(passwordEncoder.encode(bootstrapAdminPassword));
            return adminRepository.save(admin);
        }

        Admin admin = new Admin();
        admin.setName(bootstrapAdminName);
        admin.setEmail(bootstrapAdminEmail);
        admin.setPassword(passwordEncoder.encode(bootstrapAdminPassword));
        Admin saved = adminRepository.save(admin);
        System.out.println("Bootstrap admin account created (from app.admin.* properties). Email: " + bootstrapAdminEmail);
        return saved;
    }
    
    private void initializeSampleEvents(Admin owner) {
        // Sample Event 1
        Event event1 = new Event();
        event1.setTitle("AI & Machine Learning Workshop");
        event1.setDescription("Learn the fundamentals of AI and machine learning with hands-on projects.");
        event1.setCategory("workshop");
        event1.setDate(LocalDate.now().plusDays(15));
        event1.setTime(LocalTime.of(10, 0));
        event1.setVenue("Computer Lab 101");
        event1.setCapacity(50);
        event1.setPrice(new BigDecimal("25.00"));
        event1.setImage("https://images.unsplash.com/photo-1555949963-aa79dcee981c");
        event1.setOrganizer("Computer Science Department");
        event1.setContactEmail("cs@badyauni.edu");
        event1.setCreatedByAdminId(owner.getId());
        eventRepository.save(event1);
        
        // Sample Event 2
        Event event2 = new Event();
        event2.setTitle("Basketball Championship Finals");
        event2.setDescription("Watch the exciting finals between Engineering vs Business teams.");
        event2.setCategory("sports");
        event2.setDate(LocalDate.now().plusDays(20));
        event2.setTime(LocalTime.of(15, 0));
        event2.setVenue("University Sports Complex");
        event2.setCapacity(200);
        event2.setPrice(new BigDecimal("10.00"));
        event2.setImage("https://images.unsplash.com/photo-1574623452334-1e0ac2b3ccb4");
        event2.setOrganizer("Sports Department");
        event2.setContactEmail("sports@badyauni.edu");
        event2.setCreatedByAdminId(owner.getId());
        eventRepository.save(event2);
        
        // Sample Event 3
        Event event3 = new Event();
        event3.setTitle("Cultural Diversity Festival");
        event3.setDescription("Celebrate cultures from around the world with food, music, and performances.");
        event3.setCategory("cultural");
        event3.setDate(LocalDate.now().plusDays(25));
        event3.setTime(LocalTime.of(18, 0));
        event3.setVenue("Main Auditorium");
        event3.setCapacity(300);
        event3.setPrice(new BigDecimal("15.00"));
        event3.setImage("https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3");
        event3.setOrganizer("Student Affairs");
        event3.setContactEmail("culture@badyauni.edu");
        event3.setCreatedByAdminId(owner.getId());
        eventRepository.save(event3);
        
        // Sample Event 4
        Event event4 = new Event();
        event4.setTitle("Entrepreneurship Seminar");
        event4.setDescription("Learn from successful entrepreneurs about starting and growing businesses.");
        event4.setCategory("seminar");
        event4.setDate(LocalDate.now().plusDays(30));
        event4.setTime(LocalTime.of(14, 0));
        event4.setVenue("Business Lecture Hall");
        event4.setCapacity(100);
        event4.setPrice(new BigDecimal("20.00"));
        event4.setImage("https://images.unsplash.com/photo-1556761175-b413da4baf72");
        event4.setOrganizer("Business Department");
        event4.setContactEmail("business@badyauni.edu");
        event4.setCreatedByAdminId(owner.getId());
        eventRepository.save(event4);
        
        // Sample Event 5
        Event event5 = new Event();
        event5.setTitle("Advanced Mathematics Lecture");
        event5.setDescription("Explore advanced mathematical concepts and their applications in modern science.");
        event5.setCategory("academic");
        event5.setDate(LocalDate.now().plusDays(10));
        event5.setTime(LocalTime.of(11, 0));
        event5.setVenue("Mathematics Building Room 205");
        event5.setCapacity(80);
        event5.setPrice(new BigDecimal("0.00"));
        event5.setImage("https://images.unsplash.com/photo-1635070041078-e363dbe005cb");
        event5.setOrganizer("Mathematics Department");
        event5.setContactEmail("math@badyauni.edu");
        event5.setCreatedByAdminId(owner.getId());
        eventRepository.save(event5);
        
        // Sample Event 6
        Event event6 = new Event();
        event6.setTitle("Networking Social Event");
        event6.setDescription("Connect with fellow students, alumni, and industry professionals.");
        event6.setCategory("social");
        event6.setDate(LocalDate.now().plusDays(12));
        event6.setTime(LocalTime.of(17, 0));
        event6.setVenue("Student Center");
        event6.setCapacity(150);
        event6.setPrice(new BigDecimal("5.00"));
        event6.setImage("https://images.unsplash.com/photo-1521737604893-d14cc237f11d");
        event6.setOrganizer("Career Services");
        event6.setContactEmail("career@badyauni.edu");
        event6.setCreatedByAdminId(owner.getId());
        eventRepository.save(event6);
        
        System.out.println("Sample events initialized successfully!");
    }

    private void ensureUser1ProfileQrPath() {
        // If user id=1 exists, set its profile QR path to the provided image.
        userRepository.findById(1L).ifPresent(u -> {
            if (u.getProfileQrPath() == null || u.getProfileQrPath().isBlank()) {
                u.setProfileQrPath("/qr/profile_1.png");
                userRepository.save(u);
            }
        });
    }
}

