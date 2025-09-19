package fr.airsen.api.repository;

import fr.airsen.api.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByRegionId(Long regionId);
}
