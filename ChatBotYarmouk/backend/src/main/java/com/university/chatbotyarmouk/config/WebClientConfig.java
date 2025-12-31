package com.university.chatbotyarmouk.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClientConfig - HTTP Client Configuration for YU ChatBot
 *
 * PURPOSE:
 * This class configures WebClient for making HTTP requests to external APIs:
 * - Google Gemini API (LLM for generating responses)
 * - Embedding API (for creating vector embeddings)
 * - External web crawling (fetching pages from yu.edu.jo)
 *
 * WHY WEBCLIENT OVER RESTTEMPLATE?
 *
 * RestTemplate (older approach):
 * - Synchronous/blocking
 * - Thread waits until response arrives
 * - Can exhaust thread pool under load
 * - Deprecated since Spring 5
 *
 * WebClient (modern approach):
 * - Non-blocking/reactive
 * - Thread released while waiting for response
 * - Better resource utilization
 * - Supports both sync and async patterns
 * - Recommended for new projects
 *
 * REAL-LIFE ANALOGY:
 *
 * RestTemplate = Phone call
 * - You wait on the line until the person answers
 * - Can only make one call at a time
 *
 * WebClient = Text message
 * - Send message and do other things
 * - Get notified when reply arrives
 * - Can send many messages simultaneously
 *
 * @Slf4j: Lombok annotation that creates a logger named 'log'
 * Equivalent to: private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Slf4j
@Configuration
public class WebClientConfig {

    /*
     * ==================== CONFIGURATION PROPERTIES ====================
     * These values are loaded from application.properties
     */

    /**
     * Google Gemini API base URL
     *
     * Default: https://generativelanguage.googleapis.com/v1beta
     *
     * Endpoints used:
     * - /models/gemini-pro:generateContent (text generation)
     * - /models/embedding-001:embedContent (embeddings)
     */
    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    /**
     * Gemini API Key
     *
     * Obtain from: https://makersuite.google.com/app/apikey
     * Store securely - NEVER commit to version control!
     */
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    /**
     * Connection timeout in milliseconds
     *
     * How long to wait when establishing TCP connection.
     * If exceeded, throws ConnectTimeoutException.
     *
     * Default: 10000ms (10 seconds)
     */
    @Value("${webclient.connection-timeout:10000}")
    private int connectionTimeout;

    /**
     * Read timeout in milliseconds
     *
     * How long to wait for response data.
     * Important for LLM APIs which can be slow.
     *
     * Default: 60000ms (60 seconds) - LLMs need time to generate
     */
    @Value("${webclient.read-timeout:60000}")
    private int readTimeout;

    /**
     * Write timeout in milliseconds
     *
     * How long to wait when sending request data.
     * Usually quick, but can be slow for large payloads.
     *
     * Default: 10000ms (10 seconds)
     */
    @Value("${webclient.write-timeout:10000}")
    private int writeTimeout;

    /**
     * Maximum in-memory buffer size in bytes
     *
     * WebClient buffers responses in memory.
     * Large responses (like long LLM outputs) need larger buffers.
     *
     * Default: 16MB (16 * 1024 * 1024 bytes)
     * Increase if you get DataBufferLimitException
     */
    @Value("${webclient.max-buffer-size:16777216}")
    private int maxBufferSize;

    /**
     * GEMINI API WEBCLIENT
     *
     * Pre-configured WebClient specifically for Gemini API calls.
     * Includes:
     * - Base URL
     * - API key header
     * - Timeouts optimized for LLM responses
     * - Logging interceptors
     *
     * Usage in GeminiService:
     * ```java
     * @Autowired
     * @Qualifier("geminiWebClient")
     * private WebClient geminiWebClient;
     *
     * Mono<String> response = geminiWebClient.post()
     *     .uri("/models/gemini-pro:generateContent")
     *     .bodyValue(requestBody)
     *     .retrieve()
     *     .bodyToMono(String.class);
     * ```
     *
     * @return Configured WebClient for Gemini API
     */
    @Bean(name = "geminiWebClient")
    public WebClient geminiWebClient() {

        log.info("Configuring Gemini WebClient with base URL: {}", geminiBaseUrl);

        return WebClient.builder()

                /*
                 * ==================== BASE URL ====================
                 * All requests will be relative to this URL
                 *
                 * Example: .uri("/models/gemini-pro:generateContent")
                 * Becomes: https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
                 */
                .baseUrl(geminiBaseUrl)

                /*
                 * ==================== DEFAULT HEADERS ====================
                 * Headers included in every request
                 */
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)

                /*
                 * ==================== HTTP CLIENT ====================
                 * Configure the underlying HTTP client (Netty)
                 */
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))

                /*
                 * ==================== BUFFER STRATEGY ====================
                 * Configure memory buffer for response handling
                 */
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(maxBufferSize)  // 16MB buffer
                        )
                        .build())

                /*
                 * ==================== LOGGING FILTERS ====================
                 * Add request/response logging for debugging
                 */
                .filter(logRequest())
                .filter(logResponse())

                .build();
    }

    /**
     * GENERAL PURPOSE WEBCLIENT
     *
     * WebClient for general HTTP requests:
     * - Web crawling (fetching pages)
     * - External API calls
     * - Downloading documents (PDF, DOCX)
     *
     * Usage:
     * ```java
     * @Autowired
     * @Qualifier("generalWebClient")
     * private WebClient webClient;
     *
     * String html = webClient.get()
     *     .uri("https://www.yu.edu.jo/page")
     *     .retrieve()
     *     .bodyToMono(String.class)
     *     .block();
     * ```
     *
     * @return Configured WebClient for general use
     */
    @Bean(name = "generalWebClient")
    public WebClient generalWebClient() {

        log.info("Configuring General WebClient");

        return WebClient.builder()

                /*
                 * ==================== DEFAULT HEADERS ====================
                 * Common headers for web crawling
                 *
                 * User-Agent: Identify as a bot (ethical crawling)
                 * Accept: What content types we can handle
                 */
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "YUChatBot/1.0 (Yarmouk University; +https://yu.edu.jo)")
                .defaultHeader(HttpHeaders.ACCEPT,
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9,ar;q=0.8")

                /*
                 * ==================== HTTP CLIENT ====================
                 * Use same timeout configuration
                 */
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))

                /*
                 * ==================== BUFFER STRATEGY ====================
                 * Large buffer for downloading documents
                 */
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(maxBufferSize)
                        )
                        .build())

                /*
                 * ==================== LOGGING FILTERS ====================
                 * Enable request/response logging
                 */
                .filter(logRequest())
                .filter(logResponse())

                .build();
    }

    /**
     * CREATE HTTP CLIENT
     *
     * Configures the underlying Netty HTTP client.
     * Netty is a high-performance async I/O framework.
     *
     * Timeout Configuration:
     *
     * 1. Connection Timeout:
     *    - TCP handshake timeout
     *    - Server must accept connection within this time
     *
     * 2. Read Timeout:
     *    - Time between data packets
     *    - If no data received for this duration, timeout
     *
     * 3. Write Timeout:
     *    - Time to send request data
     *    - Relevant for large request bodies
     *
     * 4. Response Timeout:
     *    - Total time for complete response
     *    - Includes all data transfer
     *
     * @return Configured HttpClient
     */
    private HttpClient createHttpClient() {

        return HttpClient.create()

                /*
                 * ==================== CONNECTION TIMEOUT ====================
                 * TCP connection establishment timeout
                 *
                 * ChannelOption.CONNECT_TIMEOUT_MILLIS:
                 * - Netty-specific option
                 * - Applies to initial connection only
                 */
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)

                /*
                 * ==================== RESPONSE TIMEOUT ====================
                 * Maximum time to receive complete response
                 *
                 * Important for LLM APIs:
                 * - Token generation takes time
                 * - Long responses need longer timeout
                 * - 60 seconds is usually sufficient
                 */
                .responseTimeout(Duration.ofMillis(readTimeout))

                /*
                 * ==================== CHANNEL HANDLERS ====================
                 * Add timeout handlers to the Netty pipeline
                 *
                 * ReadTimeoutHandler:
                 * - Fires timeout if no data read for specified duration
                 * - Throws ReadTimeoutException
                 *
                 * WriteTimeoutHandler:
                 * - Fires timeout if write operation takes too long
                 * - Throws WriteTimeoutException
                 */
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                )

                /*
                 * ==================== FOLLOW REDIRECTS ====================
                 * Enable automatic redirect following
                 *
                 * Important for web crawling:
                 * - Many URLs redirect to canonical form
                 * - HTTP -> HTTPS redirects
                 * - www -> non-www redirects
                 */
                .followRedirect(true)

                /*
                 * ==================== COMPRESSION ====================
                 * Enable response compression
                 *
                 * Sends Accept-Encoding: gzip, deflate
                 * Server can send compressed response
                 * WebClient automatically decompresses
                 */
                .compress(true);
    }

    /**
     * REQUEST LOGGING FILTER
     *
     * Logs outgoing HTTP requests for debugging.
     *
     * Logs:
     * - HTTP method (GET, POST, etc.)
     * - Request URL
     * - Headers (optional, be careful with sensitive data)
     *
     * Real-Life Analogy:
     * Like a security camera at the exit door:
     * - Records what goes out
     * - Helps trace issues
     *
     * @return ExchangeFilterFunction that logs requests
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {

            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}",
                        clientRequest.method(),
                        clientRequest.url());

                // Log headers (be careful with Authorization header!)
                clientRequest.headers().forEach((name, values) -> {
                    if (!name.equalsIgnoreCase("Authorization")) {
                        values.forEach(value ->
                                log.debug("Request Header: {}={}", name, value));
                    } else {
                        log.debug("Request Header: {}=[REDACTED]", name);
                    }
                });
            }

            return Mono.just(clientRequest);
        });
    }

    /**
     * RESPONSE LOGGING FILTER
     *
     * Logs incoming HTTP responses for debugging.
     *
     * Logs:
     * - HTTP status code
     * - Response headers (selected)
     *
     * Note: Does NOT log response body (could be huge)
     *
     * @return ExchangeFilterFunction that logs responses
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {

            if (log.isDebugEnabled()) {
                log.debug("Response Status: {}",
                        clientResponse.statusCode().value());

                // Log selected response headers
                clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                    if (name.equalsIgnoreCase("Content-Type") ||
                            name.equalsIgnoreCase("Content-Length")) {
                        values.forEach(value ->
                                log.debug("Response Header: {}={}", name, value));
                    }
                });
            }

            return Mono.just(clientResponse);
        });
    }
}

/*
 * ==================== USAGE EXAMPLES ====================
 *
 * 1. Calling Gemini API (in GeminiService):
 *
 * @Service
 * public class GeminiService {
 *
 *     @Autowired
 *     @Qualifier("geminiWebClient")
 *     private WebClient geminiWebClient;
 *
 *     @Value("${gemini.api.key}")
 *     private String apiKey;
 *
 *     public String generateResponse(String prompt) {
 *         String requestBody = buildRequestBody(prompt);
 *
 *         return geminiWebClient.post()
 *             .uri(uriBuilder -> uriBuilder
 *                 .path("/models/gemini-pro:generateContent")
 *                 .queryParam("key", apiKey)
 *                 .build())
 *             .bodyValue(requestBody)
 *             .retrieve()
 *             .onStatus(HttpStatusCode::isError, response ->
 *                 response.bodyToMono(String.class)
 *                     .flatMap(body -> Mono.error(new GeminiException(body))))
 *             .bodyToMono(String.class)
 *             .block();  // Blocking call - use .subscribe() for async
 *     }
 * }
 *
 * 2. Web Crawling (in CrawlerService):
 *
 * @Service
 * public class CrawlerService {
 *
 *     @Autowired
 *     @Qualifier("generalWebClient")
 *     private WebClient webClient;
 *
 *     public String fetchPage(String url) {
 *         return webClient.get()
 *             .uri(url)
 *             .retrieve()
 *             .onStatus(HttpStatusCode::isError, response ->
 *                 Mono.error(new CrawlException("Failed to fetch: " + url)))
 *             .bodyToMono(String.class)
 *             .timeout(Duration.ofSeconds(30))
 *             .onErrorResume(e -> {
 *                 log.error("Error fetching {}: {}", url, e.getMessage());
 *                 return Mono.empty();
 *             })
 *             .block();
 *     }
 * }
 */