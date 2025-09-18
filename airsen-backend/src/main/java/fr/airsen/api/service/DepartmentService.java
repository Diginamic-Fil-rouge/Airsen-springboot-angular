package fr.airsen.api.service;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.entity.Department;
import fr.airsen.api.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /**
     * Récupère un département pour un id donné.
     */
    public DepartmentDTO getDepartmentById(int id) {
        Optional<Department> opt = departmentRepository.findById(id);
        if (opt.isEmpty()) return null;
        Department d = opt.get();
        return new DepartmentDTO(
                d.getId(),
                d.getName(),
                d.getDepartmentCode(),
                d.getRegion().getRegionCode()
        );
    }

    /**
     * Récupère tous les départements d'une région donnée.
     */
    public List<DepartmentDTO> getDepartmentsByRegion(int regionId) {
        List<Department> departments = departmentRepository.findByRegionId(regionId);
        return departments.stream()
                .map(d -> new DepartmentDTO(
                        d.getId(),
                        d.getName(),
                        d.getDepartmentCode(), // <-- passe l'int directement
                        d.getRegionCode()
                ))
                .collect(Collectors.toList());
    }
}
