package fr.airsen.api.config;

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

/**
 * Configuration de la sécurité pour l'API Airsen.
 * 
 * Cette configuration définit les règles d'accès aux différents endpoints
 * de l'application avec une authentification JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Endpoints de données géographiques publics
                .requestMatchers("/regions/**", "/departments/**", "/communes/**").permitAll()
                // Endpoints de données environnementales publics
                .requestMatchers("/air-quality/**", "/weather/**").permitAll()
                // Endpoints forum publics en lecture
                .requestMatchers("/forum/categories", "/forum/categories/**", "/forum/threads/**").permitAll()
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            .build();
    }

    /**
     * Configuration du PasswordEncoder pour le hachage sécurisé des mots de passe.
     * 
     * Utilise BCrypt avec une force de 12 pour un bon équilibre entre sécurité
     * et performance.
     * 
     * @return BCryptPasswordEncoder configuré
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
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
}