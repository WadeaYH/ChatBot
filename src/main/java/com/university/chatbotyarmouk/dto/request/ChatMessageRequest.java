package com.university.chatbotyarmouk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChatMessageRequest - Data Transfer Object for Sending Chat Messages
 *
 * PURPOSE:
 * This DTO carries the user's chat message from the frontend to the backend.
 * It's the primary input for the RAG (Retrieval-Augmented Generation) system.
 *
 * MESSAGE FLOW (Real-Life Analogy):
 * Think of this like sending a question to a smart librarian:
 *
 * 1. You write your question on a card (ChatMessageRequest)
 * 2. Hand it to the librarian (POST to /api/chat/sessions/{id}/messages)
 * 3. Librarian searches for relevant books (RAG retrieval)
 * 4. Librarian formulates an answer (Gemini API)
 * 5. You receive an answer with references (ChatMessageResponse)
 *
 * RAG PROCESSING PIPELINE:
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  User Message: "What are the admission requirements for CS?"   │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  1. CREATE EMBEDDING                                            │
 * │     Convert question to vector: [0.12, 0.45, 0.78, ...]        │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  2. VECTOR SEARCH (MongoDB)                                     │
 * │     Find similar chunks from crawled YU website                 │
 * │     Returns: top 5 relevant text chunks with URLs               │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  3. BUILD PROMPT                                                │
 * │     System: "Answer only from provided context. Cite sources."  │
 * │     Context: [chunk1, chunk2, chunk3, chunk4, chunk5]          │
 * │     History: [previous messages summary]                        │
 * │     Question: "What are the admission requirements for CS?"     │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  4. CALL GEMINI API                                             │
 * │     Send prompt, receive AI-generated answer                    │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  5. RETURN RESPONSE                                             │
 * │     Answer: "For Computer Science at YU, you need..."          │
 * │     Sources: ["https://yu.edu.jo/admission", ...]              │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * CONTENT GUIDELINES:
 * - Questions should be about Yarmouk University
 * - System answers ONLY from crawled YU data
 * - If no relevant data, system says "I don't have that information"
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for sending a chat message")
public class ChatMessageRequest {

    /**
     * The user's message content.
     *
     * This is the question or statement the user wants to send to the chatbot.
     * It will be processed through the RAG pipeline to generate a response.
     *
     * Validation Rules:
     * - Required (not blank)
     * - Minimum 1 character (can be a simple "?" or "hi")
     * - Maximum 4000 characters (reasonable limit for a chat message)
     * - Whitespace is preserved (user might format their question)
     *
     * Content Guidelines:
     * - Can be a question: "What are the admission deadlines?"
     * - Can be a statement: "Tell me about the CS department"
     * - Can be a follow-up: "Can you explain more about point 2?"
     * - Can be in Arabic: "ما هي متطلبات القبول؟"
     *
     * Valid Examples:
     * - "What are the admission requirements for Computer Science?"
     * - "متى تبدأ التسجيل للفصل الثاني؟"
     * - "Tell me about scholarships"
     * - "?"  (valid, but will get clarification request)
     *
     * Invalid Examples:
     * - "" (empty)
     * - "   " (whitespace only)
     * - null
     * - [String longer than 4000 characters]
     *
     * Processing Notes:
     * - Content is NOT sanitized for HTML (we don't render as HTML)
     * - Content is trimmed before embedding
     * - Very short messages may get "Could you elaborate?" response
     * - Very long messages are truncated for embedding
     *
     * Real-Life Analogy:
     * Like the question you write on a library help desk form:
     * - Should be clear and specific
     * - Can be in any language the librarian understands
     * - Has reasonable length limits
     */
    @Schema(
            description = "The user's message content (question or statement)",
            example = "What are the admission requirements for Computer Science?",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 4000
    )
    @NotBlank(message = "Message content is required")
    @Size(
            min = 1,
            max = 4000,
            message = "Message must be between 1 and 4000 characters"
    )
    private String content;

    /**
     * Optional: Preferred language for response.
     *
     * Allows user to request response in a specific language.
     * If not specified, system responds in the same language as the question.
     *
     * Supported values:
     * - "en" (English)
     * - "ar" (Arabic)
     * - null (auto-detect from question)
     *
     * Default: null (auto-detect)
     */
    @Schema(
            description = "Preferred language for response (en/ar). " +
                    "If not specified, responds in same language as question.",
            example = "en",
            allowableValues = {"en", "ar"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 5, message = "Language code must not exceed 5 characters")
    @Pattern(
            regexp = "^(en|ar)?$",
            message = "Language must be 'en' (English) or 'ar' (Arabic)"
    )
    private String language;
}

/*
 * ==================== USAGE EXAMPLES ====================
 *
 * 1. Creating with Builder (in tests):
 * ```java
 * ChatMessageRequest request = ChatMessageRequest.builder()
 *     .content("What are the admission requirements for Computer Science?")
 *     .language("en")
 *     .build();
 * ```
 *
 * 2. Minimal Request (JSON):
 * ```json
 * {
 *   "content": "What are the admission requirements?"
 * }
 * ```
 *
 * 3. Full Request (JSON):
 * ```json
 * {
 *   "content": "ما هي متطلبات القبول للهندسة؟",
 *   "language": "ar"
 * }
 * ```
 *
 * 4. In Controller:
 * ```java
 * @PostMapping("/sessions/{sessionId}/messages")
 * public ResponseEntity<ChatMessageResponse> sendMessage(
 *         @AuthenticationPrincipal UserPrincipal principal,
 *         @PathVariable UUID sessionId,
 *         @Valid @RequestBody ChatMessageRequest request) {
 *
 *     log.info("Message received in session: {}, length: {} chars",
 *         sessionId, request.getContent().length());
 *
 *     ChatMessageResponse response = chatService.sendMessage(
 *         sessionId,
 *         principal.getId(),
 *         request.getContent(),
 *         request.getLanguage()
 *     );
 *
 *     return ResponseEntity.status(HttpStatus.CREATED).body(response);
 * }
 * ```
 *
 * 5. In ChatService:
 * ```java
 * public ChatMessageResponse sendMessage(UUID sessionId, UUID userId,
 *                                         String content, String language) {
 *     // 1. Validate session ownership
 *     ChatSession session = validateSessionAccess(sessionId, userId);
 *
 *     // 2. Save user message
 *     ChatMessage userMessage = saveUserMessage(session, content);
 *
 *     // 3. Process with RAG
 *     RagResult ragResult = ragService.process(content, session);
 *
 *     // 4. Save assistant response
 *     ChatMessage assistantMessage = saveAssistantMessage(
 *         session,
 *         ragResult.getAnswer(),
 *         ragResult.getSources()
 *     );
 *
 *     // 5. Return response
 *     return ChatMessageResponse.builder()
 *         .id(assistantMessage.getId())
 *         .role("ASSISTANT")
 *         .content(ragResult.getAnswer())
 *         .sources(ragResult.getSources())
 *         .createdAt(assistantMessage.getCreatedAt())
 *         .build();
 * }
 * ```
 *
 * ==================== FRONTEND EXAMPLE ====================
 *
 * ```javascript
 * async function sendMessage(sessionId, content, language = null) {
 *     // Show loading state
 *     showTypingIndicator();
 *
 *     try {
 *         const response = await fetch(`/api/chat/sessions/${sessionId}/messages`, {
 *             method: 'POST',
 *             headers: {
 *                 'Authorization': `Bearer ${getToken()}`,
 *                 'Content-Type': 'application/json'
 *             },
 *             body: JSON.stringify({
 *                 content: content,
 *                 language: language  // Optional
 *             })
 *         });
 *
 *         if (!response.ok) {
 *             const error = await response.json();
 *             throw new Error(error.message || 'Failed to send message');
 *         }
 *
 *         const assistantMessage = await response.json();
 *
 *         // Display assistant message
 *         displayMessage(assistantMessage);
 *
 *         // Show sources if available
 *         if (assistantMessage.sources && assistantMessage.sources.length > 0) {
 *             displaySources(assistantMessage.sources);
 *         }
 *
 *         return assistantMessage;
 *     } catch (error) {
 *         showError('Failed to get response. Please try again.');
 *         throw error;
 *     } finally {
 *         hideTypingIndicator();
 *     }
 * }
 *
 * // Usage
 * const userInput = document.getElementById('messageInput').value;
 * if (userInput.trim()) {
 *     // Display user message immediately
 *     displayUserMessage(userInput);
 *
 *     // Send and wait for response
 *     await sendMessage(currentSessionId, userInput);
 *
 *     // Clear input
 *     document.getElementById('messageInput').value = '';
 * }
 * ```
 *
 * ==================== EXAMPLE QUESTIONS & RESPONSES ====================
 *
 * Q: "What are the admission requirements for Computer Science?"
 * A: "Based on Yarmouk University's admission page, to enroll in
 *     Computer Science you need:
 *     1. High school diploma with minimum 80% average
 *     2. Math score of at least 75%
 *     ..."
 *    Sources: [https://yu.edu.jo/admission, https://yu.edu.jo/cs-dept]
 *
 * Q: "Who is the president of USA?"
 * A: "I don't have that information in the YU crawled data. I can only
 *     answer questions about Yarmouk University. Would you like to know
 *     about YU's administration instead?"
 *    Sources: []
 *
 * Q: "متى يبدأ التسجيل؟"
 * A: "وفقاً لموقع جامعة اليرموك، يبدأ التسجيل للفصل الدراسي الثاني..."
 *    Sources: [https://yu.edu.jo/registration]
 */