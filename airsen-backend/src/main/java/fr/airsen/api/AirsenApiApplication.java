package fr.airsen.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableRetry
@EnableTransactionManagement
public class AirsenApiApplication {


    public static void main(String[] args) {
        SpringApplication.run(AirsenApiApplication.class, args);
    }
}