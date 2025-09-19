package fr.airsen.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    /**
     * Configuration personnalisée d'OpenAPI pour l'API Airsen.
     * <p>
     * Définit les informations de l'API, les serveurs disponibles et
     * les schémas de sécurité JWT.
     *
     * @return Configuration OpenAPI personnalisée
     */
    @Bean
    public OpenAPI airsenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Airsen API")
                        .description("""
                                **API REST pour le monitoring de la qualité de l'air en France**
                                
                                Cette API fournit des données en temps réel sur :
                                - **Qualité de l'air** : Indices ATMO et concentrations de polluants
                                - **Météo** : Conditions actuelles et prévisions
                                - **Données géographiques** : Régions, départements et communes françaises
                                - **Gestion utilisateur** : Profils, favoris et alertes personnalisées
                                - **Forum communautaire** : Discussions et partage d'expériences
                                - **Export de données** : Génération de rapports PDF et CSV
                                
                                ## Authentification
                                L'API utilise l'authentification JWT. Pour les endpoints protégés :
                                1. Connectez-vous via `/auth/login`
                                2. Utilisez le token reçu dans l'en-tête `Authorization: Bearer {token}`
                                
                                ## Sources de données
                                - **ATMO France** : Données officielles de qualité de l'air
                                - **Open-Meteo** : Données météorologiques
                                - **INSEE** : Données géographiques et démographiques
                                """)
                        .version("1.0.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("Équipe Airsen")
                                .email("contact@airsen.fr")
                                .url("https://airsen.fr"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api/v1")
                                .description("Serveur de développement"),
                        new Server()
                                .url("https://api.airsen.fr/v1")
                                .description("Serveur de production")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Authentification JWT. Format: 'Bearer {token}'")
                        )
                );
    }
}