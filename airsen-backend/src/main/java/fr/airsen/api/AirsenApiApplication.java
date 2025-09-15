package fr.airsen.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Application principale Airsen pour le monitoring de la qualité de l'air.
 *
 * Cette application Spring Boot fournit une API REST pour consulter les données
 * environnementales (qualité de l'air, météo) des communes françaises.
 *
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
public class AirsenApiApplication {

    /**
     * Point d'entrée de l'application.
     *
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(AirsenApiApplication.class, args);
    }
}