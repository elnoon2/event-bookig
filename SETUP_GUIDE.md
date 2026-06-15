# Badya University Event Booking System - Setup Guide

## Project Overview

This is a complete event booking system with:
- **Frontend**: HTML/CSS/JavaScript (static files)
- **Backend**: Spring Boot (Java) REST API
- **Database**: MySQL

## Features

### Regular Users
- Browse events by category
- View event details
- Book tickets for events
- View booking history
- User authentication (sign up/sign in)

### Admin Users
- All regular user features
- Create new events
- Edit events (placeholder)
- Delete events
- View all bookings
- Admin authentication (separate from regular users)

## Prerequisites

1. **Java Development Kit (JDK) 17 or higher**
2. **Maven** (for building Spring Boot project)
3. **MySQL Server** (version 8.0 or higher)
4. **Web Browser** (Chrome, Firefox, Edge, etc.)

## Database Setup

1. **Start MySQL Server**

2. **Create the database and tables** by running the SQL script:

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS badyaunievents
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE badyaunievents;

-- Users table
CREATE TABLE IF NOT EXISTS users (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name         VARCHAR(100)    NOT NULL,
  email        VARCHAR(255)    NOT NULL,
  password     VARCHAR(255)    NOT NULL,
  created_at   DATETIME        NULL,
  updated_at   DATETIME        NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Admins table
CREATE TABLE IF NOT EXISTS admins (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name         VARCHAR(100)    NOT NULL,
  email        VARCHAR(255)    NOT NULL,
  password     VARCHAR(255)    NOT NULL,
  created_at   DATETIME        NULL,
  updated_at   DATETIME        NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_admins_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Events table
CREATE TABLE IF NOT EXISTS events (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title          VARCHAR(200)    NOT NULL,
  description    VARCHAR(1000)   NOT NULL,
  category       VARCHAR(255)    NOT NULL,
  date           DATE            NOT NULL,
  time           TIME            NOT NULL,
  venue          VARCHAR(255)    NOT NULL,
  capacity       INT             NOT NULL,
  price          DECIMAL(10,2)   NOT NULL,
  image          VARCHAR(500)    NULL,
  organizer      VARCHAR(255)    NOT NULL,
  contact_email  VARCHAR(255)    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_events_date (date),
  KEY idx_events_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id       BIGINT UNSIGNED NOT NULL,
  student_name   VARCHAR(100)    NOT NULL,
  student_id     VARCHAR(50)     NOT NULL,
  student_email  VARCHAR(255)    NOT NULL,
  tickets        INT             NOT NULL,
  total_amount   DECIMAL(10,2)   NOT NULL,
  booking_date   DATETIME        NOT NULL,
  PRIMARY KEY (id),
  KEY idx_bookings_event_id (event_id),
  KEY idx_bookings_student_email (student_email),
  CONSTRAINT fk_bookings_event
    FOREIGN KEY (event_id)
    REFERENCES events(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample admin account (password: admin123)
INSERT INTO admins (name, email, password, created_at, updated_at)
VALUES ('Super Admin', 'admin@badyauniversity.com', 'admin123', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;
```

3. **Update database credentials** in `backend/src/main/resources/application-prod.properties`:
   - Change `spring.datasource.username` to your MySQL username
   - Change `spring.datasource.password` to your MySQL password

## Backend Setup

1. **Navigate to backend directory:**
   ```bash
   cd PROJECT2/backend
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Run the Spring Boot application:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```
   
   Or if you want to use the development H2 database:
   ```bash
   mvn spring-boot:run
   ```

4. **Verify backend is running:**
   - Open browser: http://localhost:5000/api/events
   - You should see JSON response (empty array if no events)

## Frontend Setup

1. **Open the frontend:**
   - Simply open `PROJECT2/index.html` in your web browser
   - Or use a local web server:
     ```bash
     # Using Python
     python -m http.server 8000
     # Then open http://localhost:8000/index.html
     ```

2. **Configure API URL (if needed):**
   - If your backend runs on a different port, edit `script.js`
   - Change `API_BASE_URL` constant at the top of the file

## Usage

### For Regular Users

1. **Sign Up:**
   - Click "Sign Up" button
   - Fill in name, email, and password
   - Account will be created in the database

2. **Sign In:**
   - Click "Sign In" button
   - Enter email and password

3. **Browse Events:**
   - View all events on the main page
   - Filter by category using tabs
   - Click "View Details" to see event information

4. **Book Events:**
   - Click "View Details" on any event
   - Click "Book Now"
   - Fill in booking form (name, student ID, email, number of tickets)
   - Submit booking

5. **View Bookings:**
   - Click "Profile" in navigation
   - See all your bookings

### For Admin Users

1. **Admin Sign Up:**
   - Click "Admin" button in navigation
   - Click "Sign Up" link
   - Fill in admin details
   - Admin account will be created

2. **Admin Sign In:**
   - Click "Admin" button
   - Enter admin email and password
   - Default admin: admin@badyauniversity.com / admin123

3. **Create Events:**
   - After logging in, click "Admin Dashboard" in navigation
   - Click "Add New Event"
   - Fill in all event details:
     - Title, Description, Category
     - Date, Time, Venue
     - Capacity, Price
     - Image URL (optional)
     - Organizer, Contact Email
   - Submit form

4. **Delete Events:**
   - Logged in admins see "Delete" button on each event card
   - Click to delete event

5. **View All Bookings:**
   - Go to Admin Dashboard
   - Click "View All Bookings"
   - See all bookings from all users

6. **Make Bookings:**
   - Admins can book events just like regular users
   - Use the same booking flow

## API Endpoints

### Events
- `GET /api/events` - Get all events
- `GET /api/events/{id}` - Get event by ID
- `GET /api/events/category/{category}` - Get events by category
- `GET /api/events/upcoming` - Get upcoming events
- `POST /api/events` - Create event (regular endpoint)
- `PUT /api/events/{id}` - Update event
- `DELETE /api/events/{id}` - Delete event

### Bookings
- `POST /api/events/{eventId}/book` - Create booking
- `GET /api/events/bookings` - Get all bookings
- `GET /api/events/bookings/{id}` - Get booking by ID
- `GET /api/events/bookings/student/{email}` - Get bookings by student email
- `GET /api/events/{eventId}/bookings` - Get bookings for an event
- `GET /api/events/{eventId}/capacity` - Get available capacity
- `DELETE /api/events/bookings/{id}` - Delete booking

### Users
- `POST /api/users` - Create user (sign up)
- `POST /api/users/authenticate` - Authenticate user (sign in)
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users` - Get all users
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Admin
- `POST /api/admin` - Create admin (sign up)
- `POST /api/admin/authenticate` - Authenticate admin (sign in)
- `GET /api/admin/{id}` - Get admin by ID
- `POST /api/admin/events` - Create event (admin)
- `PUT /api/admin/events/{id}` - Update event (admin)
- `DELETE /api/admin/events/{id}` - Delete event (admin)
- `POST /api/admin/events/{eventId}/book` - Create booking (admin)
- `GET /api/admin/bookings` - Get all bookings (admin view)
- `GET /api/admin/bookings/email/{email}` - Get bookings by email

## Project Structure

```
PROJECT2/
├── backend/                          # Spring Boot backend
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/badyauniversity/eventbooking/
│   │       │       ├── config/       # Configuration (CORS, etc.)
│   │       │       ├── controller/   # REST controllers
│   │       │       ├── dto/         # Data Transfer Objects
│   │       │       ├── exception/   # Exception handlers
│   │       │       ├── model/       # Entity models (User, Admin, Event, Booking)
│   │       │       ├── repository/  # JPA repositories
│   │       │       └── service/     # Business logic services
│   │       └── resources/
│   │           ├── application.properties      # Dev config (H2)
│   │           └── application-prod.properties # Prod config (MySQL)
│   └── pom.xml                      # Maven dependencies
├── index.html                       # Main frontend page
├── profile.html                     # User profile page
├── script.js                        # Frontend JavaScript
├── styles.css                       # Frontend styles
├── server.js                        # Old Node.js server (deprecated)
└── SETUP_GUIDE.md                  # This file
```

## Troubleshooting

### Backend won't start
- Check if MySQL is running
- Verify database credentials in `application-prod.properties`
- Check if port 5000 is available
- Look at console output for error messages

### Frontend can't connect to backend
- Verify backend is running on http://localhost:5000
- Check browser console for CORS errors
- Verify `API_BASE_URL` in `script.js` matches backend URL

### Database connection errors
- Ensure MySQL server is running
- Verify database `badyaunievents` exists
- Check username/password in `application-prod.properties`
- Ensure MySQL user has proper permissions

### Events not showing
- Check if events exist in database
- Verify backend is returning data (check http://localhost:5000/api/events)
- Check browser console for JavaScript errors

## Security Notes

⚠️ **Important**: This is a development/demo project. For production:

1. **Password Hashing**: Currently passwords are stored in plain text. Use BCrypt or similar.
2. **Authentication**: Implement JWT tokens or session management.
3. **Authorization**: Add proper role-based access control.
4. **Input Validation**: Already implemented on backend, but add more client-side validation.
5. **SQL Injection**: Using JPA prevents most issues, but always validate inputs.
6. **CORS**: Currently allows all origins. Restrict in production.

## Default Admin Account

- **Email**: admin@badyauniversity.com
- **Password**: admin123

Change this password immediately in production!

## Support

For issues or questions:
1. Check console logs (browser and Spring Boot)
2. Verify database connection
3. Check API endpoints are responding
4. Review this guide for common issues

## License

This project is for educational/demonstration purposes.



