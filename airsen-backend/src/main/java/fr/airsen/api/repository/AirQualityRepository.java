package fr.airsen.api.repository;

import fr.airsen.api.entity.AirQuality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AirQualityRepository extends JpaRepository<AirQuality, Integer> {

    List<AirQuality> findAll();

    AirQuality findById(int id);

}
