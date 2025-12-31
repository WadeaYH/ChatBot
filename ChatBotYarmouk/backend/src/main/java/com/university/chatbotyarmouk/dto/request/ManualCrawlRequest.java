package com.university.chatbotyarmouk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ManualCrawlRequest - Data Transfer Object for Manual Crawl Configuration
 *
 * PURPOSE:
 * This DTO allows admins to configure and trigger a manual web crawl
 * with custom parameters. Used when the scheduled crawl isn't enough
 * or when specific content needs to be refreshed urgently.
 *
 * CRAWLING CONCEPT (Real-Life Analogy):
 * Think of web crawling like a librarian cataloging books:
 *
 * 1. SCHEDULED CRAWL (Daily routine):
 *    - Every day, librarian checks all shelves
 *    - Updates catalog with new/changed books
 *    - Removes entries for missing books
 *
 * 2. MANUAL CRAWL (Special request):
 *    - Admin says "Check the Science section NOW"
 *    - Can specify: which shelves, how deep, what to skip
 *    - Immediate execution
 *
 * CRAWL CONFIGURATION OPTIONS:
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  startUrl: Where to begin crawling                              │
 * │  Example: "https://www.yu.edu.jo/admission"                     │
 * │  Default: "https://www.yu.edu.jo" (site root)                   │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  maxPages: How many pages to crawl                              │
 * │  Prevents infinite crawling                                     │
 * │  Default: 1000 pages                                            │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  maxDepth: How many links deep to follow                        │
 * │  Depth 0 = start page only                                      │
 * │  Depth 1 = start page + directly linked pages                   │
 * │  Default: 5 levels deep                                         │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  includePatterns: Only crawl URLs matching these patterns       │
 * │  Example: [".*admission.*", ".*registration.*"]                 │
 * └─────────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  excludePatterns: Skip URLs matching these patterns             │
 * │  Example: [".*login.*", ".*logout.*", ".*\.pdf$"]              │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * USE CASES:
 * 1. Urgent Update: New admission deadlines posted
 * 2. Targeted Refresh: Only update CS department pages
 * 3. Testing: Crawl small section to test changes
 * 4. Recovery: Re-crawl after fixing a bug
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for triggering a manual crawl with custom configuration")
public class ManualCrawlRequest {

    /**
     * Starting URL for the crawl.
     *
     * The crawler will begin at this URL and follow links from there.
     * Must be within the allowed domain (yu.edu.jo).
     *
     * Validation Rules:
     * - Optional (defaults to base URL from config)
     * - Must be valid URL format
     * - Must be within yu.edu.jo domain
     * - Maximum 500 characters
     *
     * Examples:
     * - "https://www.yu.edu.jo" (full site crawl)
     * - "https://www.yu.edu.jo/admission" (admission section)
     * - "https://www.yu.edu.jo/faculties/it" (IT faculty only)
     *
     * Real-Life Analogy:
     * Like telling the librarian which shelf to start from:
     * - Start at "Science Fiction" vs "Reference" section
     * - Determines what gets cataloged
     */
    @Schema(
            description = "Starting URL for the crawl. Must be within yu.edu.jo domain. " +
                    "Defaults to site root if not specified.",
            example = "https://www.yu.edu.jo/admission",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 500
    )
    @Size(
            max = 500,
            message = "Start URL must not exceed 500 characters"
    )
    @Pattern(
            regexp = "^(https?://([a-zA-Z0-9-]+\\.)*yu\\.edu\\.jo(/.*)?)?$",
            message = "Start URL must be within yu.edu.jo domain"
    )
    private String startUrl;

    /**
     * Maximum number of pages to crawl.
     *
     * Limits the crawl to prevent:
     * - Infinite crawling loops
     * - Server overload
     * - Excessive processing time
     * - High API costs for embeddings
     *
     * Validation Rules:
     * - Optional (defaults to config value)
     * - Minimum: 1 page
     * - Maximum: 10000 pages (hard limit)
     *
     * Typical Values:
     * - 50-100: Quick targeted crawl
     * - 500-1000: Section crawl
     * - 5000+: Full site crawl
     *
     * Real-Life Analogy:
     * Like setting a limit on how many books to catalog:
     * - "Check up to 100 books today"
     * - Prevents overwhelming the system
     */
    @Schema(
            description = "Maximum number of pages to crawl. Prevents infinite crawling.",
            example = "500",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "1",
            maximum = "10000"
    )
    @Min(value = 1, message = "Must crawl at least 1 page")
    @Max(value = 10000, message = "Cannot crawl more than 10000 pages")
    private Integer maxPages;

    /**
     * Maximum depth of links to follow.
     *
     * Controls how far from the start URL the crawler goes:
     * - Depth 0: Only the start URL
     * - Depth 1: Start URL + pages linked from it
     * - Depth 2: Above + pages linked from those
     * - And so on...
     *
     * Validation Rules:
     * - Optional (defaults to config value)
     * - Minimum: 0 (start page only)
     * - Maximum: 10 (very deep, rarely needed)
     *
     * Examples:
     * - Depth 1: Good for landing page + direct links
     * - Depth 3: Typical for section crawl
     * - Depth 5+: Full site exploration
     *
     * Real-Life Analogy:
     * Like exploring a building:
     * - Depth 0: Just the lobby
     * - Depth 1: Lobby + rooms directly connected
     * - Depth 2: Those rooms + their connected rooms
     */
    @Schema(
            description = "Maximum depth of links to follow from start URL. " +
                    "0 = start page only, higher = deeper crawl.",
            example = "3",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "0",
            maximum = "10"
    )
    @Min(value = 0, message = "Depth cannot be negative")
    @Max(value = 10, message = "Depth cannot exceed 10")
    private Integer maxDepth;

    @Schema(
            description = "Regex patterns for URLs to include (whitelist). " +
                    "Only matching URLs will be crawled. Empty = include all.",
            example = "[\".*admission.*\", \".*registration.*\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(
            max = 20,
            message = "Cannot specify more than 20 include patterns"
    )
    private List<@Size(max = 200, message = "Each pattern must not exceed 200 characters") String> includePatterns;


    @Schema(
            description = "Regex patterns for URLs to exclude (blacklist). " +
                    "Matching URLs will be skipped.",
            example = "[\".*login.*\", \".*logout.*\", \".*\\\\.pdf$\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(
            max = 20,
            message = "Cannot specify more than 20 exclude patterns"
    )
    private List<@Size(max = 200, message = "Each pattern must not exceed 200 characters") String> excludePatterns;

    /**
     * Whether to re-crawl unchanged pages.
     *
     * By default, crawler skips pages whose content hash hasn't changed.
     * Setting this to true forces re-processing of all pages.
     *
     * Use Cases for forceFresh = true:
     * - After fixing a processing bug
     * - After updating embedding model
     * - When content hash check is wrong
     *
     * Default: false (skip unchanged pages)
     *
     * Real-Life Analogy:
     * Like re-cataloging books even if they haven't changed:
     * - Usually unnecessary
     * - But needed after system changes
     */
    @Schema(
            description = "If true, re-crawl pages even if content hash hasn't changed. " +
                    "Useful after processing changes.",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Boolean forceFresh;

    /**
     * Delay between requests in milliseconds.
     *
     * Controls crawl speed to be respectful to the server.
     * Higher delay = slower crawl but less server load.
     *
     * Validation Rules:
     * - Minimum: 500ms (don't hammer the server)
     * - Maximum: 10000ms (10 seconds)
     * - Default: 1000ms (1 second)
     *
     * Real-Life Analogy:
     * Like taking breaks between checking books:
     * - Too fast = exhausting / overwhelming
     * - Reasonable pace = sustainable
     */
    @Schema(
            description = "Delay between requests in milliseconds. Higher = slower but more respectful.",
            example = "1000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "500",
            maximum = "10000"
    )
    @Min(value = 500, message = "Request delay must be at least 500ms")
    @Max(value = 10000, message = "Request delay cannot exceed 10000ms")
    private Integer requestDelayMs;

    /**
     * Content types to process.
     *
     * Specifies which file types to download and extract text from.
     *
     * Supported types:
     * - "text/html" → Web pages
     * - "application/pdf" → PDF documents
     * - "application/vnd.openxmlformats-officedocument.wordprocessingml.document" → DOCX
     *
     * Default: ["text/html", "application/pdf"]
     */
    @Schema(
            description = "Content types to process (e.g., text/html, application/pdf)",
            example = "[\"text/html\", \"application/pdf\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(
            max = 10,
            message = "Cannot specify more than 10 content types"
    )
    private List<String> contentTypes;
}

/*
 * ==================== USAGE EXAMPLES ====================
 *
 * 1. Minimal Request (use all defaults):
 * ```json
 * {}
 * ```
 * This triggers a full crawl with default settings.
 *
 * 2. Targeted Crawl (admission section only):
 * ```json
 * {
 *   "startUrl": "https://www.yu.edu.jo/admission",
 *   "maxPages": 100,
 *   "maxDepth": 2,
 *   "includePatterns": [".*admission.*", ".*requirements.*"]
 * }
 * ```
 *
 * 3. Quick Test Crawl:
 * ```json
 * {
 *   "startUrl": "https://www.yu.edu.jo",
 *   "maxPages": 10,
 *   "maxDepth": 1
 * }
 * ```
 *
 * 4. Full Refresh Crawl:
 * ```json
 * {
 *   "maxPages": 5000,
 *   "maxDepth": 5,
 *   "forceFresh": true,
 *   "excludePatterns": [".*login.*", ".*logout.*", ".*print.*"]
 * }
 * ```
 *
 * 5. Faculty-Specific Crawl:
 * ```json
 * {
 *   "startUrl": "https://www.yu.edu.jo/faculties/it",
 *   "maxPages": 200,
 *   "maxDepth": 3,
 *   "includePatterns": [".*faculties/it.*"],
 *   "contentTypes": ["text/html", "application/pdf"]
 * }
 * ```
 *
 * ==================== CONTROLLER EXAMPLE ====================
 *
 * ```java
 * @PostMapping("/crawl/run")
 * public ResponseEntity<CrawlJobResponse> triggerCrawl(
 *         @AuthenticationPrincipal UserPrincipal principal,
 *         @Valid @RequestBody(required = false) ManualCrawlRequest request) {
 *
 *     log.info("Manual crawl triggered by admin: {}", principal.getEmail());
 *
 *     // Use defaults if no request body
 *     if (request == null) {
 *         request = ManualCrawlRequest.builder().build();
 *     }
 *
 *     // Start async crawl
 *     CrawlJobResponse job = crawlerService.startManualCrawl(
 *         principal.getId(),
 *         request
 *     );
 *
 *     return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
 * }
 * ```
 *
 * ==================== CRAWLER SERVICE EXAMPLE ====================
 *
 * ```java
 * public CrawlJobResponse startManualCrawl(UUID adminId, ManualCrawlRequest request) {
 *     // Check if another crawl is running
 *     if (isAnyCrawlRunning()) {
 *         throw new ConflictException("Another crawl is already running");
 *     }
 *
 *     // Create job record
 *     CrawlJob job = CrawlJob.builder()
 *         .id(UUID.randomUUID())
 *         .triggerType(TriggerType.MANUAL)
 *         .triggeredBy(adminId)
 *         .status(CrawlStatus.PENDING)
 *         .startedAt(Instant.now())
 *         .config(buildConfig(request))
 *         .build();
 *
 *     job = crawlJobRepository.save(job);
 *
 *     // Start async crawl
 *     executeCrawlAsync(job);
 *
 *     return CrawlJobMapper.toResponse(job);
 * }
 *
 * private CrawlConfig buildConfig(ManualCrawlRequest request) {
 *     return CrawlConfig.builder()
 *         .startUrl(request.getStartUrl() != null
 *             ? request.getStartUrl()
 *             : defaultStartUrl)
 *         .maxPages(request.getMaxPages() != null
 *             ? request.getMaxPages()
 *             : defaultMaxPages)
 *         .maxDepth(request.getMaxDepth() != null
 *             ? request.getMaxDepth()
 *             : defaultMaxDepth)
 *         // ... other fields
 *         .build();
 * }
 * ```
 */