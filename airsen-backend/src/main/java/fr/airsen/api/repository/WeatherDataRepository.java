package fr.airsen.api.repository;

import fr.airsen.api.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Integer> {

    List<WeatherData> findAll();

    WeatherData findById(int id);

}
