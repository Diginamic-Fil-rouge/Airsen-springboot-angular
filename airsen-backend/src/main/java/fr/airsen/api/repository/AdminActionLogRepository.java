package fr.airsen.api.repository;

import fr.airsen.api.entity.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    // We can add custom query methods here later if needed.
}
