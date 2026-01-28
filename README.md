# Assignment 23 - Spring Boot Email Verification & JWT Authentication

A backend API-based Spring Boot application that implements user registration with email verification and JWT-based authentication.

## Features

- User registration with email and password
- Email verification with time-limited unique links (10 minutes expiry)
- JWT-based authentication
- Email rate limiting (5 minutes between verification emails)
- Password encryption using BCrypt
- Comprehensive validation and error handling
- PostgreSQL database integration

## Requirements

- Java 17 or higher
- PostgreSQL database
- Gmail account with App Password for sending emails

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE assignment23_db;
```

### 2. Configure Environment Variables

Set the following environment variables or update `application.properties`:

```bash
export MAIL_USERNAME=your-gmail@gmail.com
export MAIL_PASSWORD=your-app-password
export JWT_SECRET=your-secret-key-min-256-bits
```

**To get Gmail App Password:**
1. Go to your Google Account settings
2. Enable 2-Factor Authentication
3. Go to Security > 2-Step Verification > App passwords
4. Generate a new app password for "Mail"
5. Use this password in `MAIL_PASSWORD`

### 3. Update Database Configuration

Edit `src/main/resources/application.properties` if needed:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/assignment23_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 4. Build and Run

```bash
./gradlew clean build
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## API Endpoints

### 1. Register User

**POST** `/api/auth/register`

Request Body:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response:
```json
{
  "message": "Registration successful. Please check your email to verify your account.",
  "token": null,
  "email": null
}
```

### 2. Verify Email

**GET** `/api/auth/verify?token={verification-token}`

Response:
```json
{
  "message": "Email verified successfully. You can now login.",
  "token": null,
  "email": null
}
```

### 3. Login

**POST** `/api/auth/login`

Request Body:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Success Response (if verified):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "email": "user@example.com",
  "message": "Login successful"
}
```

Error Response (if not verified):
```json
{
  "message": "Account not verified. A new verification email has been sent.",
  "timestamp": "2026-01-28T...",
  "status": 401
}
```

### 4. Access Protected Endpoint

**GET** `/api/auth/test`

Headers:
```
Authorization: Bearer {jwt-token}
```

Response:
```
This is a protected endpoint. You are authenticated!
```

## System Design

### Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│      AuthController                 │
│  - /register                        │
│  - /login                           │
│  - /verify                          │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│      AuthService                    │
│  - User registration                │
│  - Email verification               │
│  - Login with JWT generation        │
│  - Rate limiting                    │
└──────┬──────────────────────────────┘
       │
       ├──────────────┬──────────────┬──────────────┐
       ▼              ▼              ▼              ▼
┌─────────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────┐
│EmailService │ │JwtUtil   │ │TokenService  │ │Repository│
│- Send emails│ │- Generate│ │- Create token│ │- Database│
└─────────────┘ └──────────┘ └──────────────┘ └──────────┘
```

### Database Schema

**users**
- id (Primary Key)
- email (Unique, Not Null)
- password (Not Null, Encrypted)
- verified (Boolean)
- created_at (Timestamp)
- last_verification_email_sent (Timestamp)

**verification_tokens**
- id (Primary Key)
- token (Unique, Not Null)
- user_id (Foreign Key)
- expiry_date (Timestamp)
- used (Boolean)
- created_at (Timestamp)

### Security Flow

1. **Registration:**
   - User submits email and password
   - Password is encrypted with BCrypt
   - User created with `verified=false`
   - Unique verification token generated
   - Email sent with verification link
   - Token expires in 10 minutes

2. **Email Verification:**
   - User clicks link with token
   - System validates token (not expired, not used)
   - User marked as verified
   - Token marked as used

3. **Login:**
   - User submits credentials
   - System validates email and password
   - If not verified: sends new verification email (with rate limit check)
   - If verified: generates JWT token
   - Returns JWT for authenticated requests

4. **Protected Endpoints:**
   - JWT token required in Authorization header
   - Filter validates token on each request
   - User email extracted from token

## Key Implementation Details

### Email Rate Limiting
- New verification emails can only be sent after 5 minutes
- Prevents spam and abuse
- Returns error with remaining wait time

### Token Management
- Old tokens invalidated when new one is created
- Tokens expire after 10 minutes
- Used tokens cannot be reused
- Unique UUID for each token

### Security Features
- Password encryption with BCrypt
- JWT with HMAC SHA-256 signing
- Stateless authentication
- CSRF disabled (stateless API)
- Input validation on all endpoints

### Validation Rules
- Email must be valid format
- Password minimum 6 characters
- All required fields validated
- Proper error messages returned

## Testing the Application

### Using cURL

1. **Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

2. **Verify Email:**
Check your email for the verification link and click it, or use:
```bash
curl -X GET "http://localhost:8080/api/auth/verify?token={token-from-email}"
```

3. **Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

4. **Access Protected Endpoint:**
```bash
curl -X GET http://localhost:8080/api/auth/test \
  -H "Authorization: Bearer {jwt-token}"
```

### Using Postman

1. Import the endpoints
2. Set Content-Type to `application/json`
3. For protected endpoints, add Authorization header with value `Bearer {token}`

## Error Handling

The application handles various error scenarios:

- **409 Conflict:** User already exists
- **400 Bad Request:** Invalid input, expired token
- **401 Unauthorized:** Invalid credentials, unverified account
- **429 Too Many Requests:** Email rate limit exceeded
- **500 Internal Server Error:** Unexpected errors

## Grading Criteria Coverage

✅ **JWT Authentication:** Implemented with JJWT library, token generation and validation

✅ **Complete System Design:** Layered architecture with entities, repositories, services, controllers, security

✅ **Verification Link:** Unique UUID tokens, GET endpoint, 10-minute expiry, old tokens invalidated

✅ **Proper Validation:** Email format, password length, required fields, custom exceptions

✅ **Email Sending:** JavaMailSender with proper formatting and rate limiting

✅ **Database:** PostgreSQL (no in-memory database)

## Project Structure

```
src/main/java/org/ostad/assignment_23/
├── config/
│   └── SecurityConfig.java
├── controller/
│   └── AuthController.java
├── dto/
│   ├── AuthResponse.java
│   ├── ErrorResponse.java
│   ├── LoginRequest.java
│   └── RegisterRequest.java
├── entity/
│   ├── User.java
│   └── VerificationToken.java
├── exception/
│   ├── EmailRateLimitException.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidCredentialsException.java
│   ├── InvalidTokenException.java
│   └── UserAlreadyExistsException.java
├── repository/
│   ├── UserRepository.java
│   └── VerificationTokenRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java
│   └── JwtUtil.java
└── service/
    ├── AuthService.java
    ├── EmailService.java
    └── VerificationTokenService.java
```
