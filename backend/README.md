# Badya University Event Booking Backend

A comprehensive Java OOP backend application for the Badya University Event Booking System, built with Spring Boot.

## Features

- **Event Management**: Create, read, update, and delete events
- **Booking System**: Book events with capacity management
- **User Authentication**: Sign up and sign in functionality
- **RESTful API**: Clean REST API endpoints matching front-end requirements
- **Data Validation**: Input validation using Jakarta Validation
- **Exception Handling**: Global exception handling with proper error responses
- **CORS Support**: Configured for cross-origin requests
- **Database**: H2 in-memory database for development, MySQL support for production

## Technology Stack

- **Java 17**
- **Spring Boot 3.1.5**
- **Spring Data JPA**
- **H2 Database** (Development)
- **MySQL** (Production)
- **Maven** (Build Tool)

## Project Structure

```
backend/
├── src/
│   └── main/
│       ├── java/com/badyauniversity/eventbooking/
│       │   ├── model/              # Entity classes (Event, Booking, User)
│       │   ├── repository/         # Data access layer (JPA Repositories)
│       │   ├── service/            # Business logic layer
│       │   ├── controller/         # REST API controllers
│       │   ├── dto/                # Data Transfer Objects
│       │   ├── exception/          # Exception handlers
│       │   ├── config/             # Configuration classes
│       │   └── EventBookingApplication.java
│       └── resources/
│           ├── application.properties
│           └── application-prod.properties
├── pom.xml
└── README.md
```

## API Endpoints

### Events

- `GET /api/events` - Get all events
- `GET /api/events/{id}` - Get event by ID
- `GET /api/events/category/{category}` - Get events by category
- `GET /api/events/upcoming` - Get upcoming events
- `POST /api/events` - Create a new event
- `PUT /api/events/{id}` - Update an event
- `DELETE /api/events/{id}` - Delete an event

### Bookings

- `POST /api/events/{eventId}/book` - Book an event
- `GET /api/bookings` - Get all bookings
- `GET /api/bookings/{id}` - Get booking by ID
- `GET /api/bookings/student/{email}` - Get bookings by student email
- `GET /api/events/{eventId}/bookings` - Get bookings for an event
- `GET /api/events/{eventId}/capacity` - Get available capacity
- `DELETE /api/bookings/{id}` - Delete a booking

### Users

- `POST /api/users` - Create a new user (Sign Up)
- `POST /api/users/authenticate` - Authenticate user (Sign In)
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users` - Get all users
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- (Optional) MySQL 8.0 or higher for production

### Running the Application

1. **Clone/Navigate to the backend directory:**
   ```bash
   cd backend
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR file:
   ```bash
   java -jar target/event-booking-backend-1.0.0.jar
   ```

4. **Access the application:**
   - API Base URL: `http://localhost:5000`
   - H2 Console: `http://localhost:5000/h2-console`
     - JDBC URL: `jdbc:h2:mem:badyaunievents`
     - Username: `sa`
     - Password: (leave empty)

### Production Setup

1. **Update `application-prod.properties`** with your MySQL credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/badyaunievents
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

2. **Run with production profile:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

## Data Models

### Event
- `id` (Long): Unique identifier
- `title` (String): Event title
- `description` (String): Event description
- `category` (String): Event category (academic, sports, cultural, social, workshop, seminar)
- `date` (LocalDate): Event date
- `time` (LocalTime): Event time
- `venue` (String): Event venue
- `capacity` (Integer): Maximum capacity
- `price` (BigDecimal): Ticket price
- `image` (String): Image URL
- `organizer` (String): Organizer name
- `contactEmail` (String): Contact email

### Booking
- `id` (Long): Unique identifier
- `event` (Event): Associated event
- `studentName` (String): Student's full name
- `studentId` (String): Student ID
- `studentEmail` (String): Student email
- `tickets` (Integer): Number of tickets
- `totalAmount` (BigDecimal): Total booking amount
- `bookingDate` (LocalDateTime): Booking timestamp

### User
- `id` (Long): Unique identifier
- `name` (String): User's full name
- `email` (String): User's email (unique)
- `password` (String): User's password
- `createdAt` (LocalDateTime): Account creation timestamp
- `updatedAt` (LocalDateTime): Last update timestamp

## Sample Data

The application automatically initializes with sample events on first startup:
- AI & Machine Learning Workshop
- Basketball Championship Finals
- Cultural Diversity Festival
- Entrepreneurship Seminar
- Advanced Mathematics Lecture
- Networking Social Event

## Front-End Integration

The backend is designed to work seamlessly with the front-end application. The API endpoints match the front-end's expectations:

- Events are returned with `_id` field (mapped from `id` in Java)
- Booking requests match the front-end's `BookingRequestDTO` structure
- Error responses follow a consistent format

## Development Notes

- The application uses H2 in-memory database by default for easy development
- Sample data is automatically loaded on first startup
- CORS is configured to allow all origins (adjust for production)
- All endpoints return JSON responses
- Validation errors are returned in a structured format

## Building for Production

```bash
mvn clean package
```

This creates a JAR file in the `target` directory that can be deployed.

## License

This project is part of the Badya University Event Booking System.

