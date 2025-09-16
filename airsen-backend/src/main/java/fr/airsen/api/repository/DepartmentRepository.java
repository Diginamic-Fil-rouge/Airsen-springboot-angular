package fr.airsen.api.repository;

import fr.airsen.api.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    Optional<Department> findByDepartmentCode(int departmentCode);
}

