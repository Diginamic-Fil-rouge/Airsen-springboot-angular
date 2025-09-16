package fr.airsen.api.repository;

import fr.airsen.api.entity.Commune;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommuneRepository extends JpaRepository<Commune, Integer> {
    Optional<Commune> findByInseeCode(String inseeCode);
}
