# Badya University Event Booking System

A complete full-stack event booking system with user and admin functionality.

## 🚀 Quick Start

### Prerequisites
- Java JDK 17+
- Maven
- MySQL 8.0+
- Modern web browser

### Setup Steps

1. **Database Setup**
   - Start MySQL server
   - Run the SQL script in `database_schema.sql` (or see SETUP_GUIDE.md)

2. **Backend Setup**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

3. **Frontend Setup**
   - Open `index.html` in your web browser
   - Or use a local server: `python -m http.server 8000`

## 📋 Features

### Regular Users
- ✅ Browse and filter events
- ✅ View event details
- ✅ Book tickets
- ✅ View booking history
- ✅ User authentication

### Admin Users
- ✅ All regular user features
- ✅ Create new events
- ✅ Delete events
- ✅ View all bookings
- ✅ Admin authentication

## 📁 Project Structure

```
PROJECT2/
├── backend/              # Spring Boot REST API
│   ├── src/main/java/    # Java source code
│   └── pom.xml          # Maven dependencies
├── index.html           # Main frontend page
├── script.js            # Frontend JavaScript
├── styles.css           # Frontend styles
└── SETUP_GUIDE.md      # Detailed setup instructions
```

## 🔗 API Endpoints

- **Events**: `/api/events`
- **Bookings**: `/api/events/{id}/book`
- **Users**: `/api/users`
- **Admin**: `/api/admin`

See SETUP_GUIDE.md for complete API documentation.

## 👤 Default Admin Account

- Email: `admin@badyauniversity.com`
- Password: `admin123`

## 📖 Documentation

For detailed setup instructions, see [SETUP_GUIDE.md](SETUP_GUIDE.md)

## 🛠️ Technology Stack

- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Backend**: Spring Boot (Java)
- **Database**: MySQL
- **Build Tool**: Maven

## 📝 License

Educational/Demonstration Project



