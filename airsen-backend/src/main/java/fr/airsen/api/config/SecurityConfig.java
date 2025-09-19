package fr.airsen.api.config;

import fr.airsen.api.security.JwtAuthenticationEntryPoint;
import fr.airsen.api.security.JwtTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Configures the security filter chain.
     * 
     * Defines public and private endpoints with appropriate authentication rules
     * for development.
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
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html", 
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/test/**"
                ).permitAll()
                // Public authentication endpoints
                .requestMatchers("/auth/**").permitAll()
                // Public geographic data endpoints
                .requestMatchers("/regions/**", "/departments/**", "/communes/**").permitAll()
                // Public environmental data endpoints
                .requestMatchers("/air-quality/**", "/weather/**").permitAll()
                // Public read-only forum endpoints
                .requestMatchers("/forum/categories", "/forum/categories/**", "/forum/threads/**").permitAll()
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
        
        // Allowed origins - Angular frontend
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:4200",     // Angular dev server
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
}
