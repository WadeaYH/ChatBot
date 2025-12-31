package com.university.chatbotyarmouk.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;


import jo.edu.yu.chatbot.dto.request.ChatMessageRequest;
import jo.edu.yu.chatbot.dto.request.CreateSessionRequest;
import jo.edu.yu.chatbot.dto.response.ApiErrorResponse;
import jo.edu.yu.chatbot.dto.response.ChatMessageResponse;
import jo.edu.yu.chatbot.dto.response.ChatSessionResponse;
import jo.edu.yu.chatbot.security.UserPrincipal;
import jo.edu.yu.chatbot.service.ChatService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * ChatController - Chat REST API Controller
 *
 * PURPOSE:
 * This controller handles all chat-related HTTP requests:
 * - Creating new chat sessions
 * - Listing user's chat sessions
 * - Retrieving session messages
 * - Sending messages and receiving AI responses
 * - Deleting sessions
 *
 * CHAT FLOW (Real-Life Analogy):
 *
 * Think of this like a customer support chat system:
 *
 * 1. CREATE SESSION (Start New Conversation):
 *    - Customer clicks "New Chat"
 *    - System creates a conversation ticket
 *    - Returns session ID for reference
 *
 * 2. SEND MESSAGE (Ask Question):
 *    - Customer types question
 *    - System processes with RAG
 *    - AI assistant responds
 *
 * 3. LIST SESSIONS (View History):
 *    - Customer views past conversations
 *    - Can continue any previous chat
 *
 * 4. GET MESSAGES (Load Conversation):
 *    - Load all messages from a session
 *    - Display in chat interface
 *
 * RAG (Retrieval-Augmented Generation) Flow:
 * 1. User message received
 * 2. Create embedding of user's question
 * 3. Search MongoDB for similar document chunks
 * 4. Build prompt with relevant context
 * 5. Call Gemini API for response
 * 6. Return answer with source citations
 *
 * SECURITY:
 * - All endpoints require authentication
 * - Users can only access their own sessions
 * - @PreAuthorize ensures authorization
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat sessions and messaging endpoints")
@SecurityRequirement(name = "bearerAuth")  // Swagger: requires JWT token
public class ChatController {

    /*
     * ==================== DEPENDENCY INJECTION ====================
     */
    private final ChatService chatService;

    /**
     * CREATE NEW CHAT SESSION
     *
     * Creates a new conversation session for the authenticated user.
     * Each session maintains its own conversation history and context.
     *
     * HTTP Details:
     * - Method: POST (creating a resource)
     * - URL: /api/chat/sessions
     * - Auth: Required (any authenticated user)
     * - Body: Optional title for the session
     *
     * Request Example:
     * ```
     * POST /api/chat/sessions
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
     * Content-Type: application/json
     *
     * {
     *   "title": "Questions about admission"
     * }
     * ```
     *
     * Response Example (201 Created):
     * ```
     * {
     *   "id": "550e8400-e29b-41d4-a716-446655440000",
     *   "title": "Questions about admission",
     *   "startedAt": "2024-01-15T10:30:00Z",
     *   "messageCount": 0
     * }
     * ```
     *
     * @param principal Current authenticated user (injected by Spring Security)
     * @param request Optional session configuration
     * @return ResponseEntity with created session details
     *
     * Real-Life Analogy:
     * Like opening a new support ticket:
     * - You get a ticket number (session ID)
     * - Can give it a subject/title
     * - Ready to start conversation
     */
    @PostMapping("/sessions")
    @Operation(
            summary = "Create Chat Session",
            description = "Create a new chat session for the authenticated user. " +
                    "Optionally provide a title for the session."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Session created successfully",
                    content = @Content(schema = @Schema(implementation = ChatSessionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<ChatSessionResponse> createSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) CreateSessionRequest request) {

        /*
         * @AuthenticationPrincipal:
         * Spring Security automatically injects the current user.
         * UserPrincipal contains: userId, email, roles, etc.
         *
         * This is set by JwtAuthenticationFilter when it validates the token.
         */
        log.info("Creating new chat session for user: {}", principal.getId());

        ChatSessionResponse session = chatService.createSession(
                principal.getId(),
                request != null ? request.getTitle() : null
        );

        log.info("Created chat session: {} for user: {}", session.getId(), principal.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(session);
    }

    /**
     * LIST USER'S CHAT SESSIONS
     *
     * Retrieves all chat sessions belonging to the authenticated user.
     * Supports pagination for users with many sessions.
     *
     * HTTP Details:
     * - Method: GET (retrieving resources)
     * - URL: /api/chat/sessions
     * - Auth: Required
     * - Query Params: page, size, sort
     *
     * Request Example:
     * ```
     * GET /api/chat/sessions?page=0&size=10&sort=startedAt,desc
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
     * ```
     *
     * Response Example (200 OK):
     * ```
     * {
     *   "content": [
     *     {
     *       "id": "uuid-1",
     *       "title": "Questions about admission",
     *       "startedAt": "2024-01-15T10:30:00Z",
     *       "lastMessageAt": "2024-01-15T10:45:00Z",
     *       "messageCount": 5
     *     },
     *     {
     *       "id": "uuid-2",
     *       "title": "Course registration help",
     *       "startedAt": "2024-01-14T14:00:00Z",
     *       "lastMessageAt": "2024-01-14T14:30:00Z",
     *       "messageCount": 8
     *     }
     *   ],
     *   "totalElements": 15,
     *   "totalPages": 2,
     *   "number": 0,
     *   "size": 10
     * }
     * ```
     *
     * @param principal Current authenticated user
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of user's chat sessions
     *
     * Real-Life Analogy:
     * Like viewing your support ticket history:
     * - See all past conversations
     * - Sorted by most recent
     * - Can browse through pages
     */
    @GetMapping("/sessions")
    @Operation(
            summary = "List Chat Sessions",
            description = "Get all chat sessions for the authenticated user. " +
                    "Supports pagination and sorting."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sessions retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            )
    })
    public ResponseEntity<Page<ChatSessionResponse>> listSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        /*
         * @PageableDefault:
         * Configures default pagination if not specified in request.
         * - size = 20: Return 20 items per page
         * - sort = "startedAt": Sort by session start time
         * - direction = DESC: Newest first
         *
         * Client can override: ?page=0&size=10&sort=title,asc
         */
        log.debug("Listing sessions for user: {}, page: {}",
                principal.getId(), pageable.getPageNumber());

        Page<ChatSessionResponse> sessions = chatService.getUserSessions(
                principal.getId(),
                pageable
        );

        return ResponseEntity.ok(sessions);
    }

    /**
     * GET SINGLE SESSION DETAILS
     *
     * Retrieves details of a specific chat session.
     * Only the session owner can access it.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/chat/sessions/{sessionId}
     * - Auth: Required (must be session owner)
     *
     * @param principal Current authenticated user
     * @param sessionId UUID of the session to retrieve
     * @return Session details
     */
    @GetMapping("/sessions/{sessionId}")
    @Operation(
            summary = "Get Session Details",
            description = "Get details of a specific chat session."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Session retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ChatSessionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to access this session"
            )
    })
    public ResponseEntity<ChatSessionResponse> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Session UUID")
            @PathVariable UUID sessionId) {

        log.debug("Getting session: {} for user: {}", sessionId, principal.getId());

        ChatSessionResponse session = chatService.getSession(sessionId, principal.getId());

        return ResponseEntity.ok(session);
    }

    /**
     * GET SESSION MESSAGES
     *
     * Retrieves all messages from a specific chat session.
     * Messages are returned in chronological order.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/chat/sessions/{sessionId}/messages
     * - Auth: Required (must be session owner)
     *
     * Response Example:
     * ```
     * [
     *   {
     *     "id": "msg-uuid-1",
     *     "role": "USER",
     *     "content": "What are the admission requirements?",
     *     "createdAt": "2024-01-15T10:30:00Z"
     *   },
     *   {
     *     "id": "msg-uuid-2",
     *     "role": "ASSISTANT",
     *     "content": "Based on YU's admission page, the requirements are...",
     *     "sources": ["https://www.yu.edu.jo/admission"],
     *     "createdAt": "2024-01-15T10:30:05Z"
     *   }
     * ]
     * ```
     *
     * @param principal Current authenticated user
     * @param sessionId Session to get messages from
     * @return List of messages in chronological order
     *
     * Real-Life Analogy:
     * Like loading a chat history:
     * - See the entire conversation
     * - Both your messages and responses
     * - In order they were sent
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(
            summary = "Get Session Messages",
            description = "Get all messages from a chat session in chronological order."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Messages retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to access this session"
            )
    })
    public ResponseEntity<List<ChatMessageResponse>> getSessionMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {

        log.debug("Getting messages for session: {}", sessionId);

        List<ChatMessageResponse> messages = chatService.getSessionMessages(
                sessionId,
                principal.getId()
        );

        return ResponseEntity.ok(messages);
    }

    /**
     * SEND MESSAGE AND GET AI RESPONSE
     *
     * The main chat endpoint. Sends a user message and receives AI response.
     * This is where the RAG magic happens!
     *
     * HTTP Details:
     * - Method: POST (creating messages)
     * - URL: /api/chat/sessions/{sessionId}/messages
     * - Auth: Required
     * - Body: User's message content
     *
     * RAG Processing Flow:
     * 1. Save user message to database
     * 2. Create embedding of user's question
     * 3. Vector search in MongoDB for relevant chunks
     * 4. Build prompt with context + conversation history
     * 5. Call Gemini API
     * 6. Save assistant response
     * 7. Return response with sources
     *
     * Request Example:
     * ```
     * POST /api/chat/sessions/{sessionId}/messages
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
     * Content-Type: application/json
     *
     * {
     *   "content": "What are the admission requirements for Computer Science?"
     * }
     * ```
     *
     * Response Example (201 Created):
     * ```
     * {
     *   "id": "msg-uuid",
     *   "role": "ASSISTANT",
     *   "content": "Based on Yarmouk University's admission requirements,
     *               for the Computer Science program you need:\n
     *               1. High school diploma with minimum 80% average\n
     *               2. Math score of at least 75%\n
     *               3. English proficiency...",
     *   "sources": [
     *     {
     *       "url": "https://www.yu.edu.jo/admission/requirements",
     *       "title": "Admission Requirements"
     *     },
     *     {
     *       "url": "https://www.yu.edu.jo/faculties/it/cs",
     *       "title": "Computer Science Department"
     *     }
     *   ],
     *   "createdAt": "2024-01-15T10:30:05Z"
     * }
     * ```
     *
     * @param principal Current authenticated user
     * @param sessionId Session to add message to
     * @param request User's message content
     * @return AI assistant's response with sources
     *
     * Real-Life Analogy:
     * Like asking a librarian a question:
     * - You ask about a topic
     * - Librarian searches relevant books (RAG retrieval)
     * - Gives you an answer with references (sources)
     */
    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(
            summary = "Send Message",
            description = "Send a message to the chat session and receive an AI response. " +
                    "The response is generated using RAG with YU website data."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Message sent and response generated",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid message content"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to access this session"
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI service temporarily unavailable"
            )
    })
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatMessageRequest request) {

        log.info("Message received in session: {} from user: {}",
                sessionId, principal.getId());
        log.debug("Message content length: {} characters",
                request.getContent().length());

        /*
         * The ChatService.sendMessage() method:
         * 1. Validates session ownership
         * 2. Saves user message
         * 3. Calls RAG service
         * 4. Saves assistant response
         * 5. Returns the assistant message
         */
        ChatMessageResponse response = chatService.sendMessage(
                sessionId,
                principal.getId(),
                request.getContent()
        );

        log.info("Response generated for session: {}, sources: {}",
                sessionId,
                response.getSources() != null ? response.getSources().size() : 0);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * DELETE CHAT SESSION
     *
     * Deletes a chat session and all its messages.
     * Only the session owner can delete it.
     *
     * HTTP Details:
     * - Method: DELETE
     * - URL: /api/chat/sessions/{sessionId}
     * - Auth: Required (must be session owner)
     * - Response: 204 No Content
     *
     * @param principal Current authenticated user
     * @param sessionId Session to delete
     * @return Empty response with 204 status
     *
     * Real-Life Analogy:
     * Like deleting a chat conversation:
     * - All messages are removed
     * - Cannot be recovered
     * - Session ID becomes invalid
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(
            summary = "Delete Session",
            description = "Delete a chat session and all its messages. This action cannot be undone."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Session deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to delete this session"
            )
    })
    public ResponseEntity<Void> deleteSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {

        log.info("Deleting session: {} for user: {}", sessionId, principal.getId());

        chatService.deleteSession(sessionId, principal.getId());

        log.info("Session deleted: {}", sessionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * UPDATE SESSION TITLE
     *
     * Updates the title of a chat session.
     * Useful for organizing conversations.
     *
     * HTTP Details:
     * - Method: PATCH (partial update)
     * - URL: /api/chat/sessions/{sessionId}
     * - Auth: Required (must be session owner)
     *
     * Request Example:
     * ```
     * PATCH /api/chat/sessions/{sessionId}
     * Content-Type: application/json
     *
     * {
     *   "title": "Updated title for this chat"
     * }
     * ```
     *
     * @param principal Current authenticated user
     * @param sessionId Session to update
     * @param request New title
     * @return Updated session details
     */
    @PatchMapping("/sessions/{sessionId}")
    @Operation(
            summary = "Update Session",
            description = "Update the title of a chat session."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Session updated successfully",
                    content = @Content(schema = @Schema(implementation = ChatSessionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to update this session"
            )
    })
    public ResponseEntity<ChatSessionResponse> updateSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CreateSessionRequest request) {

        log.info("Updating session: {} for user: {}", sessionId, principal.getId());

        ChatSessionResponse session = chatService.updateSessionTitle(
                sessionId,
                principal.getId(),
                request.getTitle()
        );

        return ResponseEntity.ok(session);
    }

    /**
     * CLEAR SESSION MESSAGES
     *
     * Deletes all messages from a session but keeps the session itself.
     * Useful for starting fresh in the same session.
     *
     * @param principal Current authenticated user
     * @param sessionId Session to clear
     * @return Empty response with 204 status
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    @Operation(
            summary = "Clear Session Messages",
            description = "Delete all messages from a session but keep the session."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Messages cleared successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized"
            )
    })
    public ResponseEntity<Void> clearSessionMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {

        log.info("Clearing messages for session: {}", sessionId);

        chatService.clearSessionMessages(sessionId, principal.getId());

        return ResponseEntity.noContent().build();
    }
}

/*
 * ==================== FRONTEND INTEGRATION EXAMPLES ====================
 *
 * 1. Create Session:
 * ```javascript
 * async function createSession(title = null) {
 *     const response = await fetch('/api/chat/sessions', {
 *         method: 'POST',
 *         headers: {
 *             'Authorization': `Bearer ${getToken()}`,
 *             'Content-Type': 'application/json'
 *         },
 *         body: JSON.stringify({ title })
 *     });
 *     return response.json();
 * }
 * ```
 *
 * 2. Send Message:
 * ```javascript
 * async function sendMessage(sessionId, content) {
 *     const response = await fetch(`/api/chat/sessions/${sessionId}/messages`, {
 *         method: 'POST',
 *         headers: {
 *             'Authorization': `Bearer ${getToken()}`,
 *             'Content-Type': 'application/json'
 *         },
 *         body: JSON.stringify({ content })
 *     });
 *     return response.json();
 * }
 * ```
 *
 * 3. Load Chat History:
 * ```javascript
 * async function loadMessages(sessionId) {
 *     const response = await fetch(`/api/chat/sessions/${sessionId}/messages`, {
 *         headers: {
 *             'Authorization': `Bearer ${getToken()}`
 *         }
 *     });
 *     return response.json();
 * }
 * ```
 */