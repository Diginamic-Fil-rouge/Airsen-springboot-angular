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
     * Configuration de la chaîne de filtres de sécurité.
     * 
     * Définit les endpoints publics et privés avec les règles d'authentification
     * appropriées pour le développement.
     * 
     * @param http objet HttpSecurity pour la configuration
     * @return SecurityFilterChain configurée
     * @throws Exception si une erreur de configuration survient
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (pas d'authentification requise)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html", 
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/test/**"
                ).permitAll()
                // Endpoints d'authentification publics
                .requestMatchers("/auth/**").permitAll()
                // Endpoints de données géographiques publics
                .requestMatchers("/regions/**", "/departments/**", "/communes/**").permitAll()
                // Endpoints de données environnementales publics
                .requestMatchers("/air-quality/**", "/weather/**").permitAll()
                // Endpoints forum publics en lecture
                .requestMatchers("/forum/categories", "/forum/categories/**", "/forum/threads/**").permitAll()
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            // Configuration de la gestion des exceptions d'authentification
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            // Ajouter le filtre JWT avant l'authentification par formulaire
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /**
     * Configuration du PasswordEncoder pour le hachage sécurisé des mots de passe.
     * 
     * Utilise BCrypt avec une force de 8 pour un bon équilibre entre sécurité
     * et performance.
     * 
     * @return BCryptPasswordEncoder configuré
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(8);
    }

    /**
     * Configuration de l'AuthenticationManager pour l'authentification des utilisateurs.
     * 
     * @param authConfig configuration d'authentification Spring Security
     * @return AuthenticationManager configuré
     * @throws Exception si une erreur de configuration survient
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Configuration CORS pour permettre les requêtes cross-origin depuis le frontend Angular.
     * 
     * Configure les origines autorisées, les méthodes HTTP acceptées, et les en-têtes
     * nécessaires pour l'authentification JWT.
     * 
     * @return CorsConfigurationSource configurée pour le développement et la production
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origines autorisées - frontend Angular
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:4200",     // Angular dev server
            "https://*.airsen.fr",       // Production domain pattern
            "https://airsen.fr"          // Production domain
        ));
        
        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE"
        ));
        
        // En-têtes autorisés (inclut Authorization pour JWT)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "Accept", 
            "X-Requested-With",
            "Cache-Control"
        ));
        
        // En-têtes exposés au client
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Length",
            "X-Total-Count"
        ));
        
        // Permettre l'envoi de cookies/credentials
        configuration.setAllowCredentials(true);
        
        // Durée de cache pour les requêtes preflight
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}