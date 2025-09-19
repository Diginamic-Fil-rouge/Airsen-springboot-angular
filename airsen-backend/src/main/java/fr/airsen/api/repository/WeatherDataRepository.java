package fr.airsen.api.repository;

import fr.airsen.api.entity.AirQuality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherDataRepository extends JpaRepository<AirQuality, Long> {

    List<AirQuality> findAll();


}
