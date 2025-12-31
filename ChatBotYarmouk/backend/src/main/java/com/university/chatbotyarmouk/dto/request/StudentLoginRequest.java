package com.university.chatbotyarmouk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StudentLoginRequest - Data Transfer Object for Student Authentication
 *
 * PURPOSE:
 * This DTO carries student login credentials from the frontend to the backend.
 * It captures the university email and SIS (Student Information System) password
 * needed to authenticate a student.
 *
 * AUTHENTICATION FLOW (Real-Life Analogy):
 * Think of this like swiping your student ID card at the university library:
 *
 * 1. You present your card (email) and enter PIN (password)
 * 2. System checks against university database (SIS)
 * 3. If valid, you get access to student resources
 * 4. Your session is tracked (JWT token)
 *
 * SECURITY CONSIDERATIONS:
 *
 * ⚠️ IMPORTANT: This DTO carries sensitive data (password)!
 *
 * Security Measures:
 * 1. Password is NEVER logged (see @ToString.Exclude)
 * 2. Password is NEVER stored in our database
 * 3. Password is only validated against SIS, then discarded
 * 4. HTTPS ensures password is encrypted in transit
 * 5. Rate limiting prevents brute force attacks
 *
 * Password Handling Flow:
 * 1. Frontend sends password over HTTPS (encrypted)
 * 2. Backend receives password in this DTO
 * 3. Backend sends password to SIS for validation
 * 4. SIS returns success/failure
 * 5. Password is garbage collected (never stored)
 *
 * UNIVERSITY EMAIL FORMAT:
 * Yarmouk University email formats may include:
 * - 20201234@yu.edu.jo (student ID based)
 * - ahmad.hassan@yu.edu.jo (name based)
 * - student@yu.edu.jo (legacy format)
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for student login authentication")
public class StudentLoginRequest {

    /**
     * Student's university email address.
     *
     * This email will be:
     * - Used to identify the student in SIS
     * - Stored in User entity (after successful login)
     * - Used for session management
     *
     * Validation Rules:
     * - Required (not blank)
     * - Must be valid email format
     * - Should be @yu.edu.jo domain (optional - depends on policy)
     * - Maximum 255 characters
     *
     * Valid Examples:
     * - "20201234@yu.edu.jo"
     * - "ahmad.hassan@yu.edu.jo"
     *
     * Invalid Examples:
     * - "20201234" (no domain)
     * - "ahmad@gmail.com" (not university email - if enforcing domain)
     *
     * Real-Life Analogy:
     * Like your student ID number on your university card:
     * - Uniquely identifies you in the system
     * - Links to all your academic records
     */
    @Schema(
            description = "Student's university email address",
            example = "20201234@yu.edu.jo",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 255
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(
            max = 255,
            message = "Email must not exceed 255 characters"
    )
    // Optional: Enforce university domain
    // @Pattern(
    //     regexp = "^[A-Za-z0-9._%+-]+@yu\\.edu\\.jo$",
    //     message = "Please use your Yarmouk University email (@yu.edu.jo)"
    // )
    private String email;

    /**
     * Student's SIS password.
     *
     * This password:
     * - Is validated against SIS (Student Information System)
     * - Is NEVER stored in our database
     * - Is NEVER logged anywhere
     * - Is transmitted over HTTPS only
     *
     * Validation Rules:
     * - Required (not blank)
     * - Minimum 6 characters (SIS requirement)
     * - Maximum 128 characters (reasonable limit)
     * - No pattern restriction (SIS handles password policy)
     *
     * Security Notes:
     * - @ToString.Exclude prevents password from appearing in logs
     * - Password is only held in memory during authentication
     * - Consider using char[] instead of String for extra security
     *   (Strings are immutable and stay in memory until GC)
     *
     * Real-Life Analogy:
     * Like the PIN for your ATM card:
     * - Only you should know it
     * - The system verifies it but doesn't store it
     * - Multiple wrong attempts may lock your account
     *
     * ⚠️ NEVER LOG THIS FIELD!
     */
    @Schema(
            description = "Student's SIS (Student Information System) password",
            example = "********",  // Never show real password in docs
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 6,
            maxLength = 128,
            format = "password"  // Swagger UI will mask this field
    )
    @NotBlank(message = "Password is required")
    @Size(
            min = 6,
            max = 128,
            message = "Password must be between 6 and 128 characters"
    )
    private String password;

    /**
     * Custom toString that excludes password for security.
     *
     * This overrides Lombok's @Data generated toString()
     * to prevent password from being logged accidentally.
     *
     * Example output:
     * "StudentLoginRequest(email=20201234@yu.edu.jo, password=[PROTECTED])"
     *
     * @return Safe string representation without password
     */
    @Override
    public String toString() {
        return "StudentLoginRequest(" +
                "email=" + email +
                ", password=[PROTECTED]" +
                ")";
    }
}

/*
 * ==================== USAGE EXAMPLES ====================
 *
 * 1. Creating with Builder (in tests):
 * ```java
 * StudentLoginRequest request = StudentLoginRequest.builder()
 *     .email("20201234@yu.edu.jo")
 *     .password("securePassword123")
 *     .build();
 * ```
 *
 * 2. JSON Request Body:
 * ```json
 * {
 *   "email": "20201234@yu.edu.jo",
 *   "password": "securePassword123"
 * }
 * ```
 *
 * 3. In Controller:
 * ```java
 * @PostMapping("/student/login")
 * public ResponseEntity<AuthResponse> login(
 *         @Valid @RequestBody StudentLoginRequest request) {
 *
 *     // CORRECT: Log email only
 *     log.info("Login attempt for: {}", request.getEmail());
 *
 *     // WRONG: Never do this!
 *     // log.info("Login: {}", request);  // Would expose password if not for custom toString()
 *     // log.info("Password: {}", request.getPassword());  // NEVER!
 *
 *     return authService.authenticateStudent(request);
 * }
 * ```
 *
 * 4. In AuthService:
 * ```java
 * public AuthResponse authenticateStudent(StudentLoginRequest request) {
 *     // Validate against SIS
 *     SisValidationResult result = sisService.validateCredentials(
 *         request.getEmail(),
 *         request.getPassword()  // Passed to SIS, not stored
 *     );
 *
 *     if (!result.isValid()) {
 *         throw new UnauthorizedException("Invalid credentials");
 *     }
 *
 *     // Create or get user (password NOT stored)
 *     User user = userService.findOrCreateStudent(
 *         request.getEmail(),
 *         result.getStudentProfile()
 *     );
 *
 *     // Generate JWT token
 *     String token = jwtService.generateToken(user);
 *
 *     return AuthResponse.builder()
 *         .token(token)
 *         .user(UserMapper.toDto(user))
 *         .build();
 * }
 * ```
 *
 * ==================== FRONTEND EXAMPLE ====================
 *
 * ```javascript
 * async function loginStudent(email, password) {
 *     try {
 *         const response = await fetch('/api/auth/student/login', {
 *             method: 'POST',
 *             headers: {
 *                 'Content-Type': 'application/json'
 *             },
 *             body: JSON.stringify({
 *                 email: email.trim().toLowerCase(),
 *                 password: password  // Don't trim password (spaces may be intentional)
 *             })
 *         });
 *
 *         if (response.status === 401) {
 *             throw new Error('Invalid email or password');
 *         }
 *
 *         if (!response.ok) {
 *             const error = await response.json();
 *             throw new Error(error.message || 'Login failed');
 *         }
 *
 *         const data = await response.json();
 *
 *         // Store token securely
 *         sessionStorage.setItem('token', data.token);
 *         sessionStorage.setItem('user', JSON.stringify(data.user));
 *
 *         // Clear password from memory (good practice)
 *         password = null;
 *
 *         return data;
 *     } catch (error) {
 *         console.error('Login failed:', error.message);
 *         throw error;
 *     }
 * }
 * ```
 *
 * ==================== SECURITY BEST PRACTICES ====================
 *
 * 1. HTTPS Only:
 *    - Always use HTTPS in production
 *    - Configure HSTS headers
 *    - Reject HTTP requests
 *
 * 2. Rate Limiting:
 *    - Limit login attempts (e.g., 5 per minute)
 *    - Implement exponential backoff
 *    - Consider CAPTCHA after failed attempts
 *
 * 3. Account Lockout:
 *    - Lock account after X failed attempts
 *    - Notify user via email
 *    - Require password reset to unlock
 *
 * 4. Logging:
 *    - Log login attempts (success/failure)
 *    - Log IP addresses
 *    - NEVER log passwords
 *
 * 5. Token Security:
 *    - Short expiration time
 *    - Secure, HttpOnly cookies (alternative to localStorage)
 *    - Refresh token rotation
 */