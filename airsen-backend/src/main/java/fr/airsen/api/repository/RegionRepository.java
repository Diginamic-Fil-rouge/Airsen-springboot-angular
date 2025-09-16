package fr.airsen.api.repository;

import fr.airsen.api.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Integer> {
    Optional<Region> findByRegionCode(String regionCode);
}

