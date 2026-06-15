# Admin Login and Registration - Complete Setup Guide

## ✅ System Status: FULLY OPERATIONAL

All components for admin database tables, registration, and login are **fully implemented and working**.

---

## 📊 Database Tables

### Automatic Table Creation

The `admins` table is **automatically created** by Hibernate when the Spring Boot application starts.

**Configuration Location**: `backend/src/main/resources/application.properties`

```properties
spring.jpa.hibernate.ddl-auto=update
```

This ensures:
- ✅ Tables are created automatically on first startup
- ✅ Tables are updated if schema changes
- ✅ No manual SQL scripts required

### Table Structure

```sql
CREATE TABLE admins (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name         VARCHAR(100)    NOT NULL,
  email        VARCHAR(255)    NOT NULL UNIQUE,
  password     VARCHAR(255)    NOT NULL,
  created_at   DATETIME,
  updated_at   DATETIME,
  PRIMARY KEY (id)
);
```

---

## 🔐 Admin Registration (Site Registration)

### ✅ Status: ENABLED

Admin registration is **fully functional** through the website interface.

### How to Register as Admin

1. **Navigate to Homepage**: Open `index.html`
2. **Click Admin Button**: Click the red "Admin" button in the top navigation
3. **Click Sign Up**: In the admin sign-in modal, click "Sign Up"
4. **Fill Registration Form**:
   - Full Name
   - Email (must be unique)
   - Password (minimum 6 characters)
5. **Submit**: Click "Sign Up" button

### API Endpoint

```
POST /api/admin
Content-Type: application/json

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
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

### Frontend Implementation

- **File**: `script.js`
- **Function**: `handleAdminSignUp()`
- **Modal**: `admin-signup-modal` in `index.html`

---

## 🔑 Admin Login

### ✅ Status: FULLY IMPLEMENTED

Admin login is **complete and working** with proper error handling.

### How to Login as Admin

1. **Navigate to Homepage**: Open `index.html`
2. **Click Admin Button**: Click the red "Admin" button
3. **Enter Credentials**:
   - Admin Email
   - Password
4. **Click Sign In**: Submit the form

### Default Admin Account

On first application startup, a default admin is automatically created:

- **Email**: `admin@badyauniversity.com`
- **Password**: `admin123`

⚠️ **Change this password in production!**

### API Endpoint

```
POST /api/admin/authenticate
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "password123"
}
```

### Success Response (200 OK)

```json
{
  "id": 1,
  "name": "Admin Name",
  "email": "admin@example.com",
  "role": "admin"
}
```

### Error Response (401 Unauthorized)

```json
{
  "message": "Invalid email or password"
}
```

### Frontend Implementation

- **File**: `script.js`
- **Function**: `handleAdminSignIn()`
- **Modal**: `admin-signin-modal` in `index.html`
- **Error Handling**: Properly handles 401 Unauthorized responses

---

## 🏗️ Backend Implementation

### Controller

**File**: `backend/src/main/java/.../controller/AdminController.java`

- ✅ `POST /api/admin` - Create admin (registration)
- ✅ `POST /api/admin/authenticate` - Authenticate admin (login)
- ✅ Proper error handling with 401 status for authentication failures

### Service

**File**: `backend/src/main/java/.../service/AdminService.java`

- ✅ `createAdmin()` - Creates new admin account
- ✅ `authenticateAdmin()` - Validates credentials
- ✅ Email uniqueness validation
- ✅ Password validation (min 6 characters)

### Repository

**File**: `backend/src/main/java/.../repository/AdminRepository.java`

- ✅ `findByEmail()` - Find admin by email
- ✅ `existsByEmail()` - Check if email exists

### Model

**File**: `backend/src/main/java/.../model/Admin.java`

- ✅ JPA Entity with proper annotations
- ✅ Validation constraints
- ✅ Automatic timestamp management

---

## 🎨 Frontend Implementation

### HTML Modals

**File**: `index.html`

- ✅ Admin Sign In Modal (lines 224-232)
- ✅ Admin Sign Up Modal (lines 234-247)
- ✅ Modal switching between sign in and sign up

### JavaScript Handlers

**File**: `script.js`

- ✅ `handleAdminSignIn()` - Handles login (lines 722-752)
- ✅ `handleAdminSignUp()` - Handles registration (lines 754-791)
- ✅ Event listeners properly attached
- ✅ Error handling with user-friendly messages
- ✅ Automatic login after successful registration
- ✅ UI updates after login (shows admin dashboard link)

### Styling

**File**: `styles.css`

- ✅ `.admin-signin-btn` - Styled admin button with gear icon
- ✅ Hover effects and transitions
- ✅ Responsive design

---

## 🚀 Quick Start Guide

### 1. Start the Backend

```bash
cd PROJECT2/backend
mvn spring-boot:run
```

The application will:
- ✅ Create database tables automatically
- ✅ Create default admin account (if none exists)
- ✅ Start on port 5000

### 2. Open the Frontend

Open `PROJECT2/index.html` in a web browser.

### 3. Test Admin Login

1. Click the "Admin" button
2. Use default credentials:
   - Email: `admin@badyauniversity.com`
   - Password: `admin123`
3. Click "Sign In"

### 4. Test Admin Registration

1. Click the "Admin" button
2. Click "Sign Up"
3. Fill in the form with new admin details
4. Submit

---

## ✅ Verification Checklist

- [x] Database tables created automatically
- [x] Admin registration endpoint working
- [x] Admin login endpoint working
- [x] Frontend registration form functional
- [x] Frontend login form functional
- [x] Error handling implemented
- [x] Default admin account created
- [x] Admin dashboard accessible after login
- [x] Admin can create events
- [x] Admin can view all bookings

---

## 🔧 Troubleshooting

### Admin Login Not Working

1. **Check Backend is Running**
   - Verify: `http://localhost:5000/api/admin/authenticate`
   - Should return 401 if credentials are wrong

2. **Check Default Admin Exists**
   - Email: `admin@badyauniversity.com`
   - Password: `admin123`

3. **Check Browser Console**
   - Look for JavaScript errors
   - Check network requests in DevTools

4. **Verify API URL**
   - Check `API_BASE_URL` in `script.js` (should be `http://localhost:5000`)

### Registration Not Working

1. **Check Email Uniqueness**
   - Email must be unique
   - Error message will show if email already exists

2. **Check Password Length**
   - Minimum 6 characters required

3. **Check Backend Logs**
   - Look for validation errors
   - Check for database connection issues

---

## 📝 Summary

✅ **Database Tables**: Automatically created by Hibernate  
✅ **Admin Registration**: Fully enabled via website  
✅ **Admin Login**: Complete with proper error handling  
✅ **Default Admin**: Created automatically on first startup  
✅ **Frontend Integration**: Complete with modals and handlers  
✅ **Error Handling**: Proper HTTP status codes and user messages  

**Everything is ready to use!** Admins can register and login through the website interface.

---

## 🔒 Security Notes

⚠️ **For Production**:

1. **Password Hashing**: Currently passwords are stored in plain text. Use BCrypt.
2. **JWT Tokens**: Implement token-based authentication.
3. **Rate Limiting**: Add rate limiting to prevent brute force attacks.
4. **HTTPS**: Use HTTPS in production.
5. **Input Validation**: Additional client-side validation recommended.

---

## 📚 Related Files

- `backend/src/main/java/.../controller/AdminController.java`
- `backend/src/main/java/.../service/AdminService.java`
- `backend/src/main/java/.../repository/AdminRepository.java`
- `backend/src/main/java/.../model/Admin.java`
- `backend/src/main/java/.../config/DataInitializer.java`
- `PROJECT2/script.js` (frontend handlers)
- `PROJECT2/index.html` (admin modals)
- `PROJECT2/database_schema.sql` (manual SQL if needed)

---

**Last Updated**: All components verified and working ✅



