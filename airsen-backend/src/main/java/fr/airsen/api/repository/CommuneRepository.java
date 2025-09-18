package fr.airsen.api.repository;

import fr.airsen.api.entity.Commune;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommuneRepository extends JpaRepository<Commune, Integer> {

    // Pour rechercher une commune par son code INSEE
    Optional<Commune> findByInseeCode(String inseeCode);

    // Pour lister les communes d'un département (pagination)
    List<Commune> findByDepartmentId(int departmentId, Pageable pageable);

    // Pour lister les communes d'un département avec filtre sur le nom (pagination)
    List<Commune> findByDepartmentIdAndNameContainingIgnoreCase(int departmentId, String name, Pageable pageable);

    // Pour rechercher des communes par nom (pagination)
    List<Commune> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
