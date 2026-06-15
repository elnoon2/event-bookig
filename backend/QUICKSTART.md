# Quick Start Guide

## Prerequisites

- Java 17 or higher installed
- Maven 3.6+ installed (or use Maven wrapper)

## Quick Start

1. **Navigate to the backend directory:**
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

4. **Verify the application is running:**
   - Open your browser and go to: `http://localhost:5000/api/events`
   - You should see a JSON array with sample events

## Testing the API

### Get All Events
```bash
curl http://localhost:5000/api/events
```

### Book an Event
```bash
curl -X POST http://localhost:5000/api/events/1/book \
  -H "Content-Type: application/json" \
  -d '{
    "studentName": "John Doe",
    "studentId": "STU001",
    "studentEmail": "john@example.com",
    "tickets": 2
  }'
```

### Create a User (Sign Up)
```bash
curl -X POST http://localhost:5000/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

### Authenticate User (Sign In)
```bash
curl -X POST http://localhost:5000/api/users/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

## Front-End Integration

The backend runs on port 5000 by default, which matches the front-end's expectations. Make sure:

1. The backend is running on `http://localhost:5000`
2. CORS is enabled (already configured)
3. The front-end points to the correct API endpoint

## Troubleshooting

### Port Already in Use
If port 5000 is already in use, update `application.properties`:
```properties
server.port=5001
```

### Database Issues
The application uses H2 in-memory database by default. If you see database errors:
- Check that H2 dependency is in `pom.xml`
- Clear the `target` directory and rebuild

### Maven Issues
If Maven is not found, you can use the Maven wrapper (if available) or install Maven.

## Next Steps

- Review the full [README.md](README.md) for detailed documentation
- Check API endpoints in the controllers
- Customize the application properties for your environment

