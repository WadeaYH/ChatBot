package com.university.chatbotyarmouk.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import jo.edu.yu.chatbot.dto.request.GuestAuthRequest;
import jo.edu.yu.chatbot.dto.request.StudentLoginRequest;
import jo.edu.yu.chatbot.dto.response.ApiErrorResponse;
import jo.edu.yu.chatbot.dto.response.AuthResponse;
import jo.edu.yu.chatbot.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - Authentication REST API Controller
 *
 * PURPOSE:
 * This controller handles all authentication-related HTTP requests:
 * - Guest user registration/login (name + email)
 * - Student login (university email + SIS password)
 * - Token refresh (optional)
 * - Logout (optional)
 *
 * AUTHENTICATION FLOW (Real-Life Analogy):
 *
 * Think of this like a university building entrance:
 *
 * 1. GUEST FLOW (Visitor Badge):
 *    - Visitor arrives at reception
 *    - Provides name and email
 *    - Receives temporary visitor badge (JWT token)
 *    - Can access public areas only
 *
 * 2. STUDENT FLOW (Student ID Card):
 *    - Student swipes university card
 *    - System verifies against SIS database
 *    - Receives full access badge (JWT token with STUDENT role)
 *    - Can access student-only areas (GPA, classes, etc.)
 *
 * REST API DESIGN PRINCIPLES:
 * - Use HTTP methods correctly (POST for create/auth)
 * - Return appropriate status codes (200, 201, 400, 401)
 * - Use consistent response format
 * - Validate all inputs
 * - Never expose sensitive data in responses
 *
 * ANNOTATIONS EXPLAINED:
 * @RestController = @Controller + @ResponseBody (returns JSON, not views)
 * @RequestMapping = Base URL path for all endpoints in this controller
 * @Tag = Swagger/OpenAPI documentation grouping
 * @Slf4j = Lombok logger (creates 'log' variable)
 * @RequiredArgsConstructor = Constructor injection for final fields
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Guest and Student authentication endpoints")
public class AuthController {

    /*
     * ==================== DEPENDENCY INJECTION ====================
     *
     * AuthService is injected via constructor (RequiredArgsConstructor).
     * This is the preferred way over @Autowired field injection because:
     * 1. Makes dependencies explicit
     * 2. Enables easier unit testing
     * 3. Ensures immutability (final fields)
     * 4. Fails fast if dependency is missing
     */
    private final AuthService authService;

    /**
     * GUEST AUTHENTICATION ENDPOINT
     *
     * Allows visitors to access the chatbot without a university account.
     * Creates a new guest user or returns existing user's token.
     *
     * HTTP Details:
     * - Method: POST (creating a session/resource)
     * - URL: /api/auth/guest
     * - Body: JSON with name and email
     * - Response: JWT token and user info
     *
     * Request Example:
     * ```
     * POST /api/auth/guest
     * Content-Type: application/json
     *
     * {
     *   "name": "Ahmad Hassan",
     *   "email": "ahmad@gmail.com"
     * }
     * ```
     *
     * Response Example (201 Created):
     * ```
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIs...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 86400,
     *   "user": {
     *     "id": "uuid-here",
     *     "name": "Ahmad Hassan",
     *     "email": "ahmad@gmail.com",
     *     "type": "GUEST"
     *   }
     * }
     * ```
     *
     * @param request GuestAuthRequest containing name and email
     * @return ResponseEntity with AuthResponse containing JWT token
     *
     * Real-Life Analogy:
     * Like signing in as a guest at a hotel:
     * - You give your name and contact info
     * - You get a room key card (JWT token)
     * - The card only opens certain doors (limited access)
     */
    @PostMapping("/guest")
    @Operation(
            summary = "Guest Authentication",
            description = "Authenticate as a guest user with name and email. " +
                    "Creates new guest account if email doesn't exist, " +
                    "or returns token for existing guest."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Successfully authenticated as guest",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<AuthResponse> authenticateGuest(
            @Valid @RequestBody GuestAuthRequest request) {

        /*
         * LOGGING BEST PRACTICES:
         * - Log at appropriate level (info for successful operations)
         * - Include relevant context (email for tracking)
         * - Never log sensitive data (passwords, tokens)
         * - Use parameterized logging (not string concatenation)
         */
        log.info("Guest authentication request for email: {}", request.getEmail());

        /*
         * DELEGATION TO SERVICE LAYER:
         * - Controllers should be thin (minimal logic)
         * - Business logic belongs in Service layer
         * - Controller only handles HTTP concerns
         */
        AuthResponse response = authService.authenticateGuest(request);

        log.info("Guest authentication successful for email: {}", request.getEmail());

        /*
         * HTTP STATUS CODES:
         * - 200 OK: General success
         * - 201 Created: Resource created (new user/session)
         * - 400 Bad Request: Validation error
         * - 401 Unauthorized: Authentication failed
         * - 403 Forbidden: Authenticated but not authorized
         *
         * We use 201 because we're creating a new session.
         */
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * STUDENT LOGIN ENDPOINT
     *
     * Authenticates students using their university email and SIS password.
     * Validates credentials against the Student Information System (SIS).
     *
     * HTTP Details:
     * - Method: POST
     * - URL: /api/auth/student/login
     * - Body: JSON with email and password
     * - Response: JWT token with STUDENT role
     *
     * Security Considerations:
     * - Password is validated against SIS, never stored
     * - Failed attempts should be rate-limited (implement later)
     * - Consider account lockout after multiple failures
     *
     * Request Example:
     * ```
     * POST /api/auth/student/login
     * Content-Type: application/json
     *
     * {
     *   "email": "20201234@yu.edu.jo",
     *   "password": "sisPassword123"
     * }
     * ```
     *
     * Response Example (200 OK):
     * ```
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIs...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 86400,
     *   "user": {
     *     "id": "uuid-here",
     *     "name": "Ahmad Hassan",
     *     "email": "20201234@yu.edu.jo",
     *     "type": "STUDENT",
     *     "studentId": "20201234"
     *   }
     * }
     * ```
     *
     * @param request StudentLoginRequest containing email and password
     * @return ResponseEntity with AuthResponse containing JWT token
     *
     * Real-Life Analogy:
     * Like swiping your student ID at the library:
     * - You provide your credentials
     * - System checks against university database
     * - You get access to student resources
     */
    @PostMapping("/student/login")
    @Operation(
            summary = "Student Login",
            description = "Authenticate as a student using university email and SIS password. " +
                    "Validates credentials against Student Information System."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully authenticated as student",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication failed - invalid credentials",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<AuthResponse> loginStudent(
            @Valid @RequestBody StudentLoginRequest request) {

        /*
         * SECURITY: Never log passwords!
         * Only log non-sensitive identifiers.
         */
        log.info("Student login attempt for email: {}", request.getEmail());

        AuthResponse response = authService.authenticateStudent(request);

        log.info("Student login successful for email: {}", request.getEmail());

        /*
         * 200 OK for login (not creating new resource, just authenticating)
         */
        return ResponseEntity.ok(response);
    }

    /**
     * TOKEN REFRESH ENDPOINT (Optional Enhancement)
     *
     * Allows clients to get a new token before the current one expires.
     * This provides seamless user experience without re-login.
     *
     * HTTP Details:
     * - Method: POST
     * - URL: /api/auth/refresh
     * - Header: Authorization: Bearer <current-token>
     * - Response: New JWT token
     *
     * Why Token Refresh?
     * - JWT tokens have expiration for security
     * - Users shouldn't re-login frequently
     * - Refresh extends session without credentials
     *
     * Security Considerations:
     * - Only refresh valid (not expired) tokens
     * - Consider refresh token rotation
     * - Implement token blacklisting for logout
     *
     * @param authHeader Authorization header with current token
     * @return ResponseEntity with new AuthResponse
     *
     * Real-Life Analogy:
     * Like extending a library book loan:
     * - You show your current receipt (old token)
     * - If valid, you get a new due date (new token)
     * - Don't need to re-check-out the book
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh Token",
            description = "Get a new JWT token using the current valid token. " +
                    "Extends the session without requiring re-authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("Authorization") String authHeader) {

        log.debug("Token refresh request received");

        /*
         * Extract token from "Bearer <token>" format
         *
         * Authorization header format: "Bearer eyJhbGciOiJIUzI1NiIs..."
         * We need to extract just the token part after "Bearer "
         */
        String token = extractTokenFromHeader(authHeader);

        AuthResponse response = authService.refreshToken(token);

        log.debug("Token refresh successful");

        return ResponseEntity.ok(response);
    }

    /**
     * LOGOUT ENDPOINT (Optional Enhancement)
     *
     * Invalidates the current token, ending the user's session.
     *
     * HTTP Details:
     * - Method: POST
     * - URL: /api/auth/logout
     * - Header: Authorization: Bearer <token>
     * - Response: 204 No Content
     *
     * Implementation Options:
     * 1. Token Blacklist: Store invalidated tokens until expiry
     * 2. Token Version: Increment user's token version, invalidating old tokens
     * 3. Short Expiry: Use very short token expiry + refresh tokens
     *
     * @param authHeader Authorization header with token to invalidate
     * @return ResponseEntity with no content
     *
     * Real-Life Analogy:
     * Like returning your visitor badge when leaving:
     * - Badge is deactivated
     * - Cannot be used to re-enter
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Invalidate the current token and end the session."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Successfully logged out"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Logout request received");

        String token = extractTokenFromHeader(authHeader);

        authService.logout(token);

        log.info("Logout successful");

        /*
         * 204 No Content: Success but no response body
         * Appropriate for DELETE and logout operations
         */
        return ResponseEntity.noContent().build();
    }

    /**
     * VALIDATE TOKEN ENDPOINT
     *
     * Checks if a token is valid without performing any action.
     * Useful for frontend to verify session status.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/auth/validate
     * - Header: Authorization: Bearer <token>
     * - Response: 200 OK if valid, 401 if invalid
     *
     * @param authHeader Authorization header with token
     * @return ResponseEntity with validation result
     */
    @GetMapping("/validate")
    @Operation(
            summary = "Validate Token",
            description = "Check if the provided token is valid."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token is valid"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token is invalid or expired"
            )
    })
    public ResponseEntity<Void> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = extractTokenFromHeader(authHeader);

        boolean isValid = authService.validateToken(token);

        if (isValid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
     * ==================== HELPER METHODS ====================
     */

    /**
     * Extracts JWT token from Authorization header.
     *
     * Expected format: "Bearer eyJhbGciOiJIUzI1NiIs..."
     * Returns: "eyJhbGciOiJIUzI1NiIs..."
     *
     * @param authHeader Full Authorization header value
     * @return Token string without "Bearer " prefix
     * @throws IllegalArgumentException if header format is invalid
     */
    private String extractTokenFromHeader(String authHeader) {
        /*
         * Validation:
         * 1. Header must not be null or empty
         * 2. Must start with "Bearer "
         * 3. Must have token after "Bearer "
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException(
                    "Invalid Authorization header. Expected format: 'Bearer <token>'"
            );
        }

        String token = authHeader.substring(7); // Remove "Bearer " (7 characters)

        if (token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }

        return token;
    }
}

/*
 * ==================== FRONTEND INTEGRATION EXAMPLES ====================
 *
 * 1. Guest Login (JavaScript):
 * ```javascript
 * async function loginAsGuest(name, email) {
 *     const response = await fetch('/api/auth/guest', {
 *         method: 'POST',
 *         headers: {
 *             'Content-Type': 'application/json'
 *         },
 *         body: JSON.stringify({ name, email })
 *     });
 *
 *     if (response.ok) {
 *         const data = await response.json();
 *         sessionStorage.setItem('token', data.token);
 *         sessionStorage.setItem('user', JSON.stringify(data.user));
 *         return data;
 *     } else {
 *         throw new Error('Authentication failed');
 *     }
 * }
 * ```
 *
 * 2. Student Login (JavaScript):
 * ```javascript
 * async function loginAsStudent(email, password) {
 *     const response = await fetch('/api/auth/student/login', {
 *         method: 'POST',
 *         headers: {
 *             'Content-Type': 'application/json'
 *         },
 *         body: JSON.stringify({ email, password })
 *     });
 *
 *     if (response.ok) {
 *         const data = await response.json();
 *         sessionStorage.setItem('token', data.token);
 *         return data;
 *     } else if (response.status === 401) {
 *         throw new Error('Invalid credentials');
 *     } else {
 *         throw new Error('Login failed');
 *     }
 * }
 * ```
 *
 * 3. Using Token in Requests:
 * ```javascript
 * async function authenticatedRequest(url, options = {}) {
 *     const token = sessionStorage.getItem('token');
 *
 *     const response = await fetch(url, {
 *         ...options,
 *         headers: {
 *             ...options.headers,
 *             'Authorization': `Bearer ${token}`,
 *             'Content-Type': 'application/json'
 *         }
 *     });
 *
 *     // Handle token expiry
 *     if (response.status === 401) {
 *         // Try to refresh token or redirect to login
 *         await refreshToken();
 *         return authenticatedRequest(url, options); // Retry
 *     }
 *
 *     return response;
 * }
 * ```
 */