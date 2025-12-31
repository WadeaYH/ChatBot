package com.university.chatbotyarmouk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GuestAuthRequest - Data Transfer Object for Guest Authentication
 *
 * PURPOSE:
 * This DTO (Data Transfer Object) carries guest authentication data from
 * the frontend to the backend. It captures the minimal information needed
 * to identify a guest user: name and email.
 *
 * WHAT IS A DTO? (Real-Life Analogy):
 * Think of a DTO like a standardized form you fill out:
 * - When you visit a building as a guest, you fill out a visitor form
 * - The form has specific fields (name, email, purpose)
 * - Security checks the form is filled correctly (validation)
 * - The form is then processed to create your visitor badge
 *
 * Similarly, GuestAuthRequest is like that visitor form:
 * - Frontend fills it with user input
 * - Backend validates all fields
 * - If valid, creates guest account and returns JWT token
 *
 * WHY USE DTOs?
 * 1. Separation of Concerns: Keep API contract separate from entities
 * 2. Security: Don't expose internal entity structure
 * 3. Validation: Validate input before processing
 * 4. Flexibility: API and database can evolve independently
 * 5. Documentation: Clear contract for API consumers
 *
 * VALIDATION ANNOTATIONS EXPLAINED:
 * - @NotNull: Field cannot be null
 * - @NotBlank: Field cannot be null, empty, or whitespace only
 * - @Size: String length constraints
 * - @Email: Must be valid email format
 * - @Pattern: Must match regex pattern
 *
 * LOMBOK ANNOTATIONS EXPLAINED:
 * @Data = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
 * @Builder = Enables builder pattern: GuestAuthRequest.builder().name("x").build()
 * @NoArgsConstructor = Creates no-args constructor (needed for JSON deserialization)
 * @AllArgsConstructor = Creates all-args constructor (needed for @Builder)
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for guest user authentication")
public class GuestAuthRequest {

    /**
     * Guest's display name.
     *
     * This name will be:
     * - Displayed in the chat interface
     * - Stored in the User entity
     * - Used in greetings ("Hello, Ahmad!")
     *
     * Validation Rules:
     * - Required (not blank)
     * - Minimum 2 characters (real names are at least 2 chars)
     * - Maximum 100 characters (database column limit)
     * - Trimmed of leading/trailing whitespace
     *
     * Valid Examples: "Ahmad", "Sara Hassan", "محمد علي"
     * Invalid Examples: "", "   ", "A", null
     *
     * Real-Life Analogy:
     * Like the "Name" field on a visitor badge:
     * - Cannot be empty (security needs to identify you)
     * - Has reasonable length limits
     */
    @Schema(
            description = "Guest's display name",
            example = "Ahmad Hassan",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 100
    )
    @NotBlank(message = "Name is required")
    @Size(
            min = 2,
            max = 100,
            message = "Name must be between 2 and 100 characters"
    )
    private String name;

    /**
     * Guest's email address.
     *
     * This email will be:
     * - Used as unique identifier for the guest
     * - Used to link sessions if guest returns
     * - NOT used for sending emails (unless feature added later)
     *
     * Validation Rules:
     * - Required (not blank)
     * - Must be valid email format (user@domain.tld)
     * - Maximum 255 characters (standard email length limit)
     * - Case-insensitive (Ahmad@Gmail.com = ahmad@gmail.com)
     *
     * Valid Examples: "ahmad@gmail.com", "user123@yu.edu.jo"
     * Invalid Examples: "ahmad", "@gmail.com", "ahmad@", "ahmad@.com"
     *
     * Real-Life Analogy:
     * Like the "Contact Email" on a visitor form:
     * - Must be a real email format
     * - Used to identify returning visitors
     *
     * Note: @Email annotation uses standard email regex.
     * For stricter validation, use @Pattern with custom regex.
     */
    @Schema(
            description = "Guest's email address (used as unique identifier)",
            example = "ahmad.hassan@gmail.com",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 255
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(
            max = 255,
            message = "Email must not exceed 255 characters"
    )
    private String email;
}

/*
 * ==================== USAGE EXAMPLES ====================
 *
 * 1. Creating with Builder (in tests):
 * ```java
 * GuestAuthRequest request = GuestAuthRequest.builder()
 *     .name("Ahmad Hassan")
 *     .email("ahmad@gmail.com")
 *     .build();
 * ```
 *
 * 2. JSON Deserialization (from HTTP request):
 * ```json
 * {
 *   "name": "Ahmad Hassan",
 *   "email": "ahmad@gmail.com"
 * }
 * ```
 * Automatically converted to GuestAuthRequest by Spring's Jackson.
 *
 * 3. In Controller:
 * ```java
 * @PostMapping("/guest")
 * public ResponseEntity<AuthResponse> authenticate(
 *         @Valid @RequestBody GuestAuthRequest request) {
 *     // @Valid triggers validation
 *     // If validation fails, Spring returns 400 Bad Request
 *     // with details about which fields failed
 * }
 * ```
 *
 * 4. Validation Error Response (automatic):
 * ```json
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "errors": [
 *     {
 *       "field": "name",
 *       "message": "Name is required"
 *     },
 *     {
 *       "field": "email",
 *       "message": "Please provide a valid email address"
 *     }
 *   ]
 * }
 * ```
 *
 * ==================== FRONTEND EXAMPLE ====================
 *
 * ```javascript
 * async function authenticateGuest(name, email) {
 *     const response = await fetch('/api/auth/guest', {
 *         method: 'POST',
 *         headers: {
 *             'Content-Type': 'application/json'
 *         },
 *         body: JSON.stringify({
 *             name: name.trim(),      // Trim whitespace
 *             email: email.trim().toLowerCase()  // Normalize email
 *         })
 *     });
 *
 *     if (!response.ok) {
 *         const error = await response.json();
 *         // Handle validation errors
 *         if (error.errors) {
 *             error.errors.forEach(e => {
 *                 console.error(`${e.field}: ${e.message}`);
 *             });
 *         }
 *         throw new Error(error.message);
 *     }
 *
 *     return response.json();
 * }
 * ```
 */