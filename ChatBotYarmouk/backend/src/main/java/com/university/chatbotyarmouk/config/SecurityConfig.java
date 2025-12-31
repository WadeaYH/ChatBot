package com.university.chatbotyarmouk.config;

import com.university.chatbotyarmouk.security.CustomUserDetailsService;
import com.university.chatbotyarmouk.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig - Spring Security Configuration for YU ChatBot
 *
 * PURPOSE:
 * This class configures the security settings for the application including:
 * - JWT-based stateless authentication (no session cookies)
 * - Role-based access control (GUEST, STUDENT, ADMIN)
 * - CORS configuration integration
 * - CSRF protection disabled (appropriate for stateless REST APIs)
 *
 * SECURITY FLOW (Real-Life Analogy - Airport Security):
 * Think of this like airport security:
 * 1. JwtAuthenticationFilter = Security checkpoint (checks your boarding pass/JWT)
 * 2. AuthenticationProvider = Verification system (validates your identity)
 * 3. Authorization Rules = Different areas (Public lobby, Boarding gates, VIP lounge)
 *
 * REQUEST FLOW:
 * 1. Request arrives → JwtAuthenticationFilter intercepts
 * 2. Filter extracts JWT from "Authorization: Bearer <token>" header
 * 3. If valid, SecurityContext is populated with user details
 * 4. Request proceeds to controller with authenticated principal
 *
 * ANNOTATIONS EXPLAINED:
 * @Configuration - Tells Spring this class contains bean definitions
 * @EnableWebSecurity - Activates Spring Security's web protection
 * @EnableMethodSecurity - Allows @PreAuthorize on controller methods
 * @RequiredArgsConstructor - Lombok generates constructor for final fields
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    /*
     * ==================== DEPENDENCY INJECTION ====================
     * These are injected automatically by Spring via constructor.
     * RequiredArgsConstructor creates:
     * public SecurityConfig(JwtAuthenticationFilter filter, CustomUserDetailsService service) {
     *     this.jwtAuthenticationFilter = filter;
     *     this.userDetailsService = service;
     * }
     */

    /** Custom filter that extracts and validates JWT tokens from requests */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Service that loads user data from database for authentication */
    private final CustomUserDetailsService userDetailsService;

    /**
     * SECURITY FILTER CHAIN CONFIGURATION
     *
     * This is the MAIN security configuration method.
     * It defines the entire security pipeline for HTTP requests.
     *
     * Real-Life Analogy:
     * Like setting up rules for a building:
     * - Main entrance (/) = Public, anyone can enter
     * - Guest rooms (/api/chat) = Need any valid key card
     * - Student labs (/api/student) = Need student key card
     * - Admin office (/api/admin) = Need admin key card only
     *
     * @param http HttpSecurity builder object
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /*
                 * ==================== CSRF CONFIGURATION ====================
                 * CSRF = Cross-Site Request Forgery
                 *
                 * WHY DISABLED?
                 * - CSRF attacks exploit session cookies
                 * - We use JWT tokens (not cookies) for authentication
                 * - JWT tokens must be explicitly included in requests
                 * - This makes CSRF attacks impossible
                 *
                 * Real-Life Analogy:
                 * CSRF is like someone forging your signature on a form.
                 * JWT is like requiring a personal PIN for every transaction.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                 * ==================== CORS CONFIGURATION ====================
                 * CORS = Cross-Origin Resource Sharing
                 *
                 * WHY NEEDED?
                 * - Frontend runs on different origin (e.g., localhost:3000)
                 * - Backend runs on another origin (e.g., localhost:8080)
                 * - Browsers block cross-origin requests by default
                 * - CORS headers tell browser to allow these requests
                 *
                 * Real-Life Analogy:
                 * Like international calling - you need permission to call
                 * numbers in different countries.
                 */
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();

                    // Allow requests from any origin (restrict in production!)
                    corsConfig.addAllowedOriginPattern("*");

                    // Allow all HTTP methods: GET, POST, PUT, DELETE, etc.
                    corsConfig.addAllowedMethod("*");

                    // Allow all headers including Authorization
                    corsConfig.addAllowedHeader("*");

                    // Allow credentials (cookies, authorization headers)
                    corsConfig.setAllowCredentials(true);

                    // Cache preflight response for 1 hour (3600 seconds)
                    // Reduces OPTIONS requests for better performance
                    corsConfig.setMaxAge(3600L);

                    return corsConfig;
                }))

                /*
                 * ==================== AUTHORIZATION RULES ====================
                 * Define WHO can access WHAT endpoints
                 *
                 * Order matters! First matching rule wins.
                 * More specific rules should come before general ones.
                 */
                .authorizeHttpRequests(auth -> auth

                        // --------- PUBLIC ENDPOINTS (No Authentication) ---------

                        // Authentication endpoints: login, register, guest auth
                        // Everyone needs access to these to GET a token
                        .requestMatchers("/api/auth/**").permitAll()

                        // Health check for monitoring systems (e.g., Kubernetes, AWS)
                        .requestMatchers("/api/health").permitAll()

                        // API documentation (Swagger UI)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Static frontend files
                        .requestMatchers("/", "/index.html", "/styles.css", "/app.js").permitAll()
                        .requestMatchers("/assets/**").permitAll()

                        // --------- ROLE-BASED ENDPOINTS ---------

                        // ADMIN only: crawl management, system config
                        // hasRole("ADMIN") checks for ROLE_ADMIN authority
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // STUDENT only: GPA, classes, attendance, profile
                        .requestMatchers("/api/student/**").hasRole("STUDENT")

                        // ANY authenticated user: chat functionality
                        // Guests, Students, and Admins can all chat
                        .requestMatchers("/api/chat/**").authenticated()

                        // --------- DEFAULT RULE ---------
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                /*
                 * ==================== SESSION MANAGEMENT ====================
                 * Configure how sessions are handled
                 *
                 * STATELESS means:
                 * - No HTTP session created on server
                 * - No session cookies sent to client
                 * - Each request must contain JWT token
                 * - Better scalability (no session storage needed)
                 *
                 * Real-Life Analogy:
                 * Like showing your ID at every door vs getting a wristband.
                 * Stateless = Show ID every time (JWT in every request)
                 * Stateful = Get wristband once (session cookie)
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /*
                 * ==================== AUTHENTICATION PROVIDER ====================
                 * Set the component that validates credentials
                 */
                .authenticationProvider(authenticationProvider())

                /*
                 * ==================== FILTER CHAIN ORDER ====================
                 * Add JWT filter BEFORE the default username/password filter
                 *
                 * Filter execution order:
                 * 1. JwtAuthenticationFilter (our custom filter)
                 * 2. UsernamePasswordAuthenticationFilter (Spring's default)
                 * 3. Other Spring Security filters...
                 *
                 * This ensures JWT is checked first on every request.
                 */
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AUTHENTICATION PROVIDER BEAN
     *
     * DaoAuthenticationProvider connects:
     * - UserDetailsService (loads user from database)
     * - PasswordEncoder (verifies password hashes)
     *
     * Authentication Flow:
     * 1. User submits username/password
     * 2. UserDetailsService loads user by username
     * 3. PasswordEncoder compares submitted password with stored hash
     * 4. If match, authentication succeeds
     *
     * Real-Life Analogy:
     * Like a receptionist (Provider) who:
     * 1. Looks up your reservation (UserDetailsService)
     * 2. Checks your ID matches the booking (PasswordEncoder)
     *
     * @return Configured DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        // Set service that retrieves user from database
        authProvider.setUserDetailsService(userDetailsService);

        // Set encoder for password comparison
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /**
     * AUTHENTICATION MANAGER BEAN
     *
     * AuthenticationManager is the main API for authentication.
     * Used in AuthService to authenticate login requests.
     *
     * Usage Example:
     * authenticationManager.authenticate(
     *     new UsernamePasswordAuthenticationToken(email, password)
     * );
     *
     * @param config Spring's authentication configuration
     * @return AuthenticationManager instance
     * @throws Exception if creation fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * PASSWORD ENCODER BEAN
     *
     * BCrypt is the industry-standard for password hashing.
     *
     * Features:
     * - One-way hashing (cannot decrypt)
     * - Built-in salt (random data added to password)
     * - Adaptive cost factor (can be increased over time)
     * - Resistant to rainbow table attacks
     *
     * How BCrypt Works:
     * 1. Generate random salt
     * 2. Combine password + salt
     * 3. Hash using Blowfish cipher (multiple rounds)
     * 4. Store: $2a$10$salt+hash (includes algorithm version and cost)
     *
     * Real-Life Analogy:
     * Like a fingerprint scanner:
     * - You can verify someone's fingerprint (compare hashes)
     * - But you can't reconstruct the finger from the scan
     *
     * SECURITY RULE: NEVER store plain-text passwords!
     *
     * @return BCryptPasswordEncoder with default strength (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Default strength = 10 (2^10 = 1024 iterations)
        // Higher = more secure but slower
        // 10 is good balance for most applications
        // Increase to 12 for highly sensitive systems
        return new BCryptPasswordEncoder();
    }
}