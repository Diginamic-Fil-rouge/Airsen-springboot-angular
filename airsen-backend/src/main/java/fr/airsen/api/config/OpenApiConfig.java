package fr.airsen.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI airsenOpenAPI() {
        // Define the security scheme name
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Airsen API")
                .description("""
                    **REST API for air quality monitoring in France**

                    This API provides real-time data on:
                    - **Air Quality**: ATMO indices and pollutant concentrations
                    - **Weather**: Current conditions and forecasts
                    - **Geographic Data**: French regions, departments and communes
                    - **User Management**: Profiles, favorites and personalized alerts
                    - **Community Forum**: Discussions and experience sharing
                    - **Data Export**: PDF and CSV report generation
                    """)
                .version("1.0.0-SNAPSHOT")
                .contact(new Contact()
                    .name("Airsen Team")
                    .email("contact@airsen.fr")
                    .url("https://airsen.fr"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/api/v1")
                    .description("Development server"),
                new Server()
                    .url("https://api.airsen.fr/v1")
                    .description("Production server")
            ))
            // Add global security requirement
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            // Define security schemes
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT token. You can get one from /auth/login endpoint.")
                )
            );
    }
}
