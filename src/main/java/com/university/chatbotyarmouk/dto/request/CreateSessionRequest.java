package com.university.chatbotyarmouk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateSessionRequest - Data Transfer Object for Creating Chat Sessions
 *
 * PURPOSE:
 * This DTO carries optional configuration for creating a new chat session.
 * A chat session is a conversation container that holds related messages.
 *
 * SESSION CONCEPT (Real-Life Analogy):
 * Think of a chat session like a support ticket:
 *
 * 1. You open a new ticket (create session)
 * 2. Give it a subject/title (optional but helpful)
 * 3. Add messages to the ticket (chat messages)
 * 4. Can reference the ticket later (session ID)
 * 5. Can close or delete the ticket (end/delete session)
 *
 * WHY SESSIONS?
 *
 * 1. Organization:
 *    - Group related questions together
 *    - Easier to find past conversations
 *    - Clear conversation boundaries
 *
 * 2. Context:
 *    - AI uses session history for context
 *    - Follow-up questions make sense
 *    - "Can you explain more about point 2?"
 *
 * 3. Summaries:
 *    - Each session can have a summary
 *    - Used to compress long conversations
 *    - Efficient for context in new messages
 *
 * SESSION LIFECYCLE:
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  1. CREATE SESSION                                              │
 * │     POST /api/chat/sessions                                     │
 * │     Optional: title = "Questions about admission"               │
 * │     Returns: sessionId = "uuid-123"                             │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  2. SEND MESSAGES                                               │
 * │     POST /api/chat/sessions/uuid-123/messages                   │
 * │     User asks questions, AI responds                            │
 * │     Context maintained within session                           │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  3. CONTINUE LATER                                              │
 * │     GET /api/chat/sessions/uuid-123/messages                    │
 * │     Load previous messages                                       │
 * │     Continue conversation with context                           │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  4. END/DELETE SESSION (Optional)                               │
 * │     DELETE /api/chat/sessions/uuid-123                          │
 * │     Removes session and all messages                            │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * OPTIONAL VS REQUIRED:
 * All fields in this DTO are optional. If no body is provided,
 * a session is created with auto-generated title.
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a new chat session")
public class CreateSessionRequest {

    /**
     * Optional title for the chat session.
     *
     * The title helps users identify and organize their conversations.
     * If not provided, the system may auto-generate a title based on
     * the first message (like ChatGPT does).
     *
     * Validation Rules:
     * - Optional (can be null or empty)
     * - Maximum 200 characters
     * - Trimmed of leading/trailing whitespace
     *
     * Auto-Title Generation (if null):
     * - After first message, extract key topic
     * - Example: "What are admission req..." → "Admission Requirements"
     * - Keeps conversations organized without user effort
     *
     * Good Titles:
     * - "Questions about CS admission"
     * - "Scholarship inquiry"
     * - "أسئلة عن التسجيل"
     * - "Course registration help"
     *
     * Not Recommended:
     * - "Chat" (too generic)
     * - "asdfgh" (not meaningful)
     * - [Very long titles that get truncated]
     *
     * Real-Life Analogy:
     * Like the subject line of an email:
     * - Helps you find conversations later
     * - Gives quick overview of the topic
     * - Optional but recommended for organization
     */
    @Schema(
            description = "Optional title for the chat session. " +
                    "If not provided, a title may be auto-generated from the first message.",
            example = "Questions about CS admission requirements",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 200
    )
    @Size(
            max = 200,
            message = "Title must not exceed 200 characters"
    )
    private String title;

    /**
     * Optional: Initial system prompt override.
     *
     * Advanced feature allowing customization of the AI's behavior
     * for this specific session. Not typically used by regular users.
     *
     * Use Cases:
     * - Testing different prompt strategies
     * - Specialized assistants (e.g., "Answer only about IT faculty")
     * - Admin debugging
     *
     * Default: Uses standard RAG system prompt if null
     *
     * Security Note:
     * - This field may be restricted to admin users
     * - Should not allow bypassing safety guidelines
     */
    @Schema(
            description = "Optional system prompt override for this session. " +
                    "Advanced feature for customizing AI behavior.",
            example = "You are a helpful assistant specializing in YU admission queries.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 2000
    )
    @Size(
            max = 2000,
            message = "System prompt must not exceed 2000 characters"
    )
    private String systemPrompt;

    /**
     * Optional: Session metadata as JSON string.
     *
     * Allows storing additional custom data with the session.
     * Useful for analytics or custom features.
     *
     * Example metadata:
     * - {"source": "mobile-app", "topic": "admission"}
     * - {"referrer": "homepage-widget"}
     *
     * Not processed by the system, just stored.
     */
    @Schema(
            description = "Optional metadata as JSON string for custom tracking",
            example = "{\"source\": \"mobile-app\", \"topic\": \"admission\"}",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 1000
    )
    @Size(
            max = 1000,
            message = "Metadata must not exceed 1000 characters"
    )
    private String metadata;
}

/*
 * ==================== USAGE EXAMPLES ====================
 *
 * 1. Creating with Builder (in tests):
 * ```java
 * // Minimal - just title
 * CreateSessionRequest request = CreateSessionRequest.builder()
 *     .title("Questions about admission")
 *     .build();
 *
 * // Full options
 * CreateSessionRequest request = CreateSessionRequest.builder()
 *     .title("CS Admission FAQ")
 *     .systemPrompt("Focus on Computer Science department only.")
 *     .metadata("{\"source\": \"widget\"}")
 *     .build();
 * ```
 *
 * 2. Minimal JSON Request (just title):
 * ```json
 * {
 *   "title": "Questions about CS admission"
 * }
 * ```
 *
 * 3. Empty Request (auto-generate title):
 * ```json
 * {}
 * ```
 * Or simply don't send a body at all.
 *
 * 4. Full JSON Request:
 * ```json
 * {
 *   "title": "Scholarship Inquiry",
 *   "systemPrompt": "Provide detailed information about scholarships.",
 *   "metadata": "{\"source\": \"mobile-app\", \"userId\": \"guest-123\"}"
 * }
 * ```
 *
 * 5. In Controller:
 * ```java
 * @PostMapping("/sessions")
 * public ResponseEntity<ChatSessionResponse> createSession(
 *         @AuthenticationPrincipal UserPrincipal principal,
 *         @Valid @RequestBody(required = false) CreateSessionRequest request) {
 *
 *     // request can be null if no body provided
 *     String title = request != null ? request.getTitle() : null;
 *
 *     ChatSessionResponse session = chatService.createSession(
 *         principal.getId(),
 *         title
 *     );
 *
 *     return ResponseEntity.status(HttpStatus.CREATED).body(session);
 * }
 * ```
 *
 * 6. In ChatService:
 * ```java
 * public ChatSessionResponse createSession(UUID userId, String title) {
 *     User user = userRepository.findById(userId)
 *         .orElseThrow(() -> new ResourceNotFoundException("User not found"));
 *
 *     ChatSession session = ChatSession.builder()
 *         .id(UUID.randomUUID())
 *         .user(user)
 *         .title(title)  // Can be null, auto-generated later
 *         .startedAt(Instant.now())
 *         .build();
 *
 *     session = chatSessionRepository.save(session);
 *
 *     log.info("Created session {} for user {}", session.getId(), userId);
 *
 *     return ChatSessionResponse.builder()
 *         .id(session.getId())
 *         .title(session.getTitle())
 *         .startedAt(session.getStartedAt())
 *         .messageCount(0)
 *         .build();
 * }
 * ```
 *
 * ==================== FRONTEND EXAMPLE ====================
 *
 * ```javascript
 * // Create session with title
 * async function createSession(title = null) {
 *     const response = await fetch('/api/chat/sessions', {
 *         method: 'POST',
 *         headers: {
 *             'Authorization': `Bearer ${getToken()}`,
 *             'Content-Type': 'application/json'
 *         },
 *         body: title ? JSON.stringify({ title }) : null
 *     });
 *
 *     if (!response.ok) {
 *         throw new Error('Failed to create session');
 *     }
 *
 *     return response.json();
 * }
 *
 * // Usage - New Chat Button
 * document.getElementById('newChatBtn').addEventListener('click', async () => {
 *     // Option 1: Prompt for title
 *     const title = prompt('Give this chat a title (optional):');
 *
 *     // Option 2: No title (auto-generate)
 *     // const title = null;
 *
 *     try {
 *         const session = await createSession(title);
 *
 *         // Navigate to new session
 *         currentSessionId = session.id;
 *         clearChatDisplay();
 *
 *         console.log(`Created session: ${session.id}`);
 *     } catch (error) {
 *         showError('Failed to create new chat');
 *     }
 * });
 * ```
 *
 * ==================== AUTO-TITLE GENERATION ====================
 *
 * When no title is provided, the system can auto-generate one
 * after the first message. Here's how:
 *
 * ```java
 * public void autoGenerateTitle(ChatSession session, String firstMessage) {
 *     if (session.getTitle() != null) {
 *         return;  // Already has title
 *     }
 *
 *     // Option 1: Simple truncation
 *     String title = firstMessage.length() > 50
 *         ? firstMessage.substring(0, 47) + "..."
 *         : firstMessage;
 *
 *     // Option 2: Use AI to extract topic
 *     // String title = aiService.extractTopic(firstMessage);
 *
 *     // Option 3: Extract key words
 *     // String title = extractKeywords(firstMessage);
 *
 *     session.setTitle(title);
 *     chatSessionRepository.save(session);
 * }
 * ```
 */