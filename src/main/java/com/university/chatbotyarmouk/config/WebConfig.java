package com.university.chatbotyarmouk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig - Web MVC Configuration for YU ChatBot
 *
 * PURPOSE:
 * This class configures web-related settings including:
 * - CORS (Cross-Origin Resource Sharing) policies
 * - Static resource handling
 * - Request/Response customization
 *
 * WHY IMPLEMENT WebMvcConfigurer?
 * WebMvcConfigurer is an interface that allows customizing Spring MVC.
 * By implementing it, we can override specific configuration methods
 * without affecting other default configurations.
 *
 * CORS EXPLAINED (Real-Life Analogy):
 * Imagine your frontend is a person in Country A (localhost:3000)
 * and your backend is a business in Country B (localhost:8080).
 *
 * Without CORS: The border (browser) blocks all communication.
 * With CORS: You get a special permit (CORS headers) that says
 *            "Country B allows visitors from Country A."
 *
 * Browser Security Model:
 * - Same-Origin Policy: By default, browsers block requests to different origins
 * - Origin = Protocol + Domain + Port
 * - http://localhost:3000 ≠ http://localhost:8080 (different ports = different origins)
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /*
     * ==================== CONFIGURATION PROPERTIES ====================
     * These values are loaded from application.properties
     *
     * @Value annotation injects property values:
     * - ${property.name} = get value from properties file
     * - ${property.name:default} = use default if property not found
     */

    /**
     * Allowed origins for CORS requests.
     *
     * In application.properties:
     * app.cors.allowed-origins=http://localhost:3000,http://localhost:5500
     *
     * Default: * (all origins - only for development!)
     */
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    /**
     * Maximum age for CORS preflight cache (in seconds).
     *
     * Preflight = OPTIONS request sent before actual request
     * Caching reduces the number of OPTIONS requests.
     *
     * Default: 3600 seconds (1 hour)
     */
    @Value("${app.cors.max-age:3600}")
    private long corsMaxAge;

    /**
     * CORS CONFIGURATION
     *
     * This method configures CORS mappings for the entire application.
     * It defines which external origins can access which endpoints.
     *
     * CORS Request Types:
     *
     * 1. SIMPLE REQUESTS (no preflight):
     *    - GET, HEAD, POST methods
     *    - Only simple headers (Accept, Content-Type, etc.)
     *    - Content-Type: text/plain, multipart/form-data, application/x-www-form-urlencoded
     *
     * 2. PREFLIGHTED REQUESTS:
     *    - PUT, DELETE, PATCH methods
     *    - Custom headers (Authorization, X-Custom-Header)
     *    - Content-Type: application/json
     *    - Browser sends OPTIONS request first to check permissions
     *
     * Real-Life Analogy:
     * Simple Request = Walking into a public building
     * Preflight = Calling ahead to ask if you can visit the restricted area
     *
     * @param registry CorsRegistry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry
                /*
                 * ==================== PATH PATTERN ====================
                 * Which endpoints does this CORS config apply to?
                 *
                 * "/**" = All endpoints
                 * "/api/**" = Only API endpoints
                 * "/api/public/**" = Only public API endpoints
                 */
                .addMapping("/**")

                /*
                 * ==================== ALLOWED ORIGINS ====================
                 * Which domains can make requests to our API?
                 *
                 * Options:
                 * - Specific origins: "http://localhost:3000", "https://yu.edu.jo"
                 * - Pattern: "https://*.yu.edu.jo" (subdomains)
                 * - All origins: "*" (DANGEROUS in production!)
                 *
                 * Security Note:
                 * In production, ALWAYS specify exact origins.
                 * Using "*" allows any website to access your API.
                 */
                .allowedOriginPatterns(allowedOrigins.split(","))

                /*
                 * ==================== ALLOWED METHODS ====================
                 * Which HTTP methods are permitted?
                 *
                 * Common methods:
                 * - GET: Retrieve data
                 * - POST: Create new resource
                 * - PUT: Update entire resource
                 * - PATCH: Partial update
                 * - DELETE: Remove resource
                 * - OPTIONS: Preflight request (handled automatically)
                 */
                .allowedMethods(
                        "GET",      // Read operations
                        "POST",     // Create operations
                        "PUT",      // Full update operations
                        "PATCH",    // Partial update operations
                        "DELETE",   // Delete operations
                        "OPTIONS"   // Preflight requests
                )

                /*
                 * ==================== ALLOWED HEADERS ====================
                 * Which request headers can the client send?
                 *
                 * Common headers:
                 * - Authorization: JWT token (Bearer xxx)
                 * - Content-Type: Request body format (application/json)
                 * - Accept: Expected response format
                 * - X-Requested-With: AJAX identifier
                 *
                 * "*" = Allow all headers
                 */
                .allowedHeaders("*")

                /*
                 * ==================== EXPOSED HEADERS ====================
                 * Which response headers can the client read?
                 *
                 * By default, browsers only expose "simple" headers:
                 * - Cache-Control, Content-Language, Content-Type
                 * - Expires, Last-Modified, Pragma
                 *
                 * Custom headers must be explicitly exposed.
                 */
                .exposedHeaders(
                        "Authorization",      // Allow client to read auth token
                        "Content-Disposition", // For file downloads
                        "X-Total-Count",      // Custom pagination header
                        "X-Page-Number",      // Custom pagination header
                        "X-Page-Size"         // Custom pagination header
                )

                /*
                 * ==================== ALLOW CREDENTIALS ====================
                 * Should cookies and authorization headers be included?
                 *
                 * true = Include credentials (needed for JWT in Authorization header)
                 * false = Don't include credentials
                 *
                 * IMPORTANT: Cannot use allowedOrigins("*") with credentials=true
                 * Must use allowedOriginPatterns("*") instead.
                 */
                .allowCredentials(true)

                /*
                 * ==================== MAX AGE ====================
                 * How long should browsers cache the preflight response?
                 *
                 * Benefits of caching:
                 * - Reduces OPTIONS requests
                 * - Improves performance
                 * - Reduces server load
                 *
                 * Value is in seconds (3600 = 1 hour)
                 */
                .maxAge(corsMaxAge);
    }

    /**
     * STATIC RESOURCE HANDLERS
     *
     * Configure how static files (HTML, CSS, JS, images) are served.
     * This is useful when serving the frontend from the same server.
     *
     * Resource Locations:
     * - classpath:/static/ = Files in src/main/resources/static/
     * - classpath:/public/ = Files in src/main/resources/public/
     * - file:/path/to/files/ = Files on filesystem
     *
     * Real-Life Analogy:
     * Like setting up a filing cabinet:
     * - addResourceHandler = Label on the drawer ("frontend files here")
     * - addResourceLocations = Where to find the actual files
     *
     * @param registry ResourceHandlerRegistry to configure
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        /*
         * Serve frontend static files
         *
         * URL Pattern: /static/**
         * Maps to: classpath:/static/
         *
         * Example:
         * Request: GET /static/logo.png
         * Serves: src/main/resources/static/logo.png
         */
        registry
                .addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);  // Cache for 1 hour

        /*
         * Serve uploaded files (if stored locally)
         *
         * URL Pattern: /uploads/**
         * Maps to: file:./uploads/
         *
         * Example:
         * Request: GET /uploads/document.pdf
         * Serves: ./uploads/document.pdf (relative to app directory)
         */
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .setCachePeriod(86400);  // Cache for 24 hours

        /*
         * Serve favicon
         *
         * Browsers automatically request /favicon.ico
         * This mapping serves it from resources/static/
         */
        registry
                .addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico")
                .setCachePeriod(604800);  // Cache for 1 week
    }
}