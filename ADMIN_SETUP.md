# Admin Database Setup and Registration Guide

## Database Tables

The admin table is automatically created by Hibernate when the application starts. The configuration is set in `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This setting ensures that:
- Tables are automatically created if they don't exist
- Tables are updated if the schema changes
- No manual SQL scripts are required for basic setup

### Admin Table Structure

The `admins` table is created with the following structure:
- `id` (BIGINT, Primary Key, Auto-increment)
- `name` (VARCHAR(100), Required)
- `email` (VARCHAR(255), Required, Unique)
- `password` (VARCHAR(255), Required, Min 6 characters)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

## Default Admin Account

On first application startup, a default admin account is automatically created:

- **Email**: `admin@badyauniversity.com`
- **Password**: `admin123`

This default admin is only created if no admins exist in the database.

## Admin Registration (Site Registration)

Admin registration is **fully enabled** and available through the frontend. The registration endpoint is:

### API Endpoint
```
POST /api/admin
```

### Request Body
```json
{
  "name": "Admin Name",
  "email": "admin@example.com",
  "password": "password123"
}
```

### Response
```json
{
  "id": 1,
  "name": "Admin Name",
  "email": "admin@example.com",
  "createdAt": "2024-01-01T00:00:00"
}
```

## Frontend Integration

The frontend already includes admin registration functionality:

1. **Admin Sign Up Modal**: Available on the homepage
2. **Admin Sign In Modal**: Available on the homepage
3. **JavaScript Handler**: `handleAdminSignUp()` function in `script.js`

### How to Register as Admin

1. Click the "Admin" button on the homepage
2. Click "Sign Up" link in the admin sign-in modal
3. Fill in the registration form:
   - Full Name
   - Email
   - Password (minimum 6 characters)
4. Submit the form

Upon successful registration:
- The admin account is created in the database
- The admin is automatically logged in
- The admin gains access to admin features (create events, view all bookings, etc.)

## Database Setup Options

### Option 1: Automatic (Recommended for Development)
The application uses H2 in-memory database by default. Tables are created automatically on startup.

### Option 2: MySQL (Production)
For production, update `application-prod.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/badyaunievents?createDatabaseIfNotExist=true
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Option 3: Manual SQL Script
If you prefer to create tables manually, use the provided `database_schema.sql` file:

```bash
mysql -u root -p < database_schema.sql
```

## Verification

To verify the admin table was created:

1. **H2 Console** (Development):
   - Navigate to: `http://localhost:5000/h2-console`
   - JDBC URL: `jdbc:h2:mem:badyaunievents`
   - Username: `sa`
   - Password: (empty)
   - Run: `SELECT * FROM admins;`

2. **MySQL** (Production):
   ```sql
   USE badyaunievents;
   SELECT * FROM admins;
   ```

## Security Notes

⚠️ **Important**: In production, you should:
1. Hash passwords using BCrypt or similar
2. Use environment variables for database credentials
3. Enable SSL for database connections
4. Implement proper authentication tokens (JWT)
5. Add rate limiting for registration endpoints

The current implementation stores passwords in plain text for simplicity. This should be updated for production use.

## Summary

✅ **Database tables**: Automatically created by Hibernate  
✅ **Admin registration**: Fully enabled via API endpoint  
✅ **Frontend integration**: Complete with modals and handlers  
✅ **Default admin**: Created automatically on first startup  

The system is ready to use! Admins can register through the website interface.



