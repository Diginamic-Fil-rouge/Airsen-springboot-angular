package fr.airsen.api.config;

import fr.airsen.api.security.JwtAuthenticationEntryPoint;
import fr.airsen.api.security.JwtTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Hooks;

import java.util.Arrays;
import jakarta.annotation.PostConstruct;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Configures the security filter chain with role-based authorization.
     * 
     * Endpoint access rules:
     * - Public: /auth/**, /test/**, /actuator/**, Swagger docs
     * - VISITOR: Read-only forum access
     * - USER/ADMIN: Full data access, forum participation, alerts
     * - ADMIN: User management and moderation features
     * 
     * @param http HttpSecurity object for configuration
     * @return configured SecurityFilterChain
     * @throws Exception if configuration error occurs
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - Authentication not required
                .requestMatchers("/auth/login", "/auth/register", "/auth/refresh", "/auth/logout", "/auth/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                // Redis Test endpoints - Read-only public, destructive operations require ADMIN
                .requestMatchers(HttpMethod.GET, "/test/redis/connection", "/test/redis/cache-stats",
                                "/test/redis/metrics", "/test/redis/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/test/redis/keys").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/test/redis/clear-cache/**", "/test/redis/clear-cache").hasRole("ADMIN")

                // Other test endpoints (ATMO, Health, etc.) - public for integration testing
                .requestMatchers("/test/**").permitAll()


                // Forum endpoints - VISITOR can read, USER/ADMIN can write
                .requestMatchers(HttpMethod.GET, "/forum/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/forum/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/forum/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/forum/**").hasAnyRole("USER", "ADMIN")

                // Data endpoints - USER and ADMIN access required
                .requestMatchers("/regions/**", "/departments/**", "/communes/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/weather/**", "/atmo/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/alerts/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/notifications/**").hasAnyRole("USER", "ADMIN")
                
                // Admin-only endpoints (if any in future)
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Authentication exception handling configuration
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            // Add JWT filter before form authentication
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /**
     * Configures PasswordEncoder for secure password hashing.
     * 
     * Uses BCrypt with strength 8 for good balance between security
     * and performance.
     * 
     * @return configured BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(8);
    }

    /**
     * Configures AuthenticationManager for user authentication.
     * 
     * @param authConfig Spring Security authentication configuration
     * @return configured AuthenticationManager
     * @throws Exception if configuration error occurs
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * CORS configuration to allow cross-origin requests from Angular frontend.
     * 
     * Configures allowed origins, accepted HTTP methods, and headers
     * necessary for JWT authentication.
     * 
     * @return CorsConfigurationSource configured for development and production
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins - Angular frontend and Swagger UI
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:4200",     // Angular dev server
            "http://localhost:8080",     // Swagger UI
            "https://*.airsen.fr",       // Production domain pattern
            "https://airsen.fr"          // Production domain
        ));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE"
        ));
        
        // Allowed headers (includes Authorization for JWT)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "Accept", 
            "X-Requested-With",
            "Cache-Control"
        ));
        
        // Headers exposed to client
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Length",
            "X-Total-Count"
        ));
        
        // Allow sending cookies/credentials
        configuration.setAllowCredentials(true);
        
        // Cache duration for preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Enables automatic propagation of SecurityContext to reactive threads.
     * 
     * This is crucial for reactive endpoints to maintain authentication
     * across async operations and thread boundaries in servlet-based applications.
     */
    @PostConstruct
    public void enableReactiveSecurityContextPropagation() {
        // Enable automatic security context propagation for reactive streams
        // This ensures SecurityContext from servlet threads is available in reactive streams
        Hooks.enableAutomaticContextPropagation();
        
        // Configure SecurityContextHolder for async thread propagation
        // MODE_INHERITABLETHREADLOCAL ensures JWT authentication context is preserved
        // when reactive operators execute on different threads
        org.springframework.security.core.context.SecurityContextHolder
            .setStrategyName(org.springframework.security.core.context.SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}