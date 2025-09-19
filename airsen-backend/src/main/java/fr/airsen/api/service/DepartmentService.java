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

    public DepartmentDTO getDepartmentById(Long id) {
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

    public List<DepartmentDTO> getDepartmentsByRegion(Long regionId) {
        List<Department> departments = departmentRepository.findByRegionId(regionId);
        return departments.stream()
                .map(d -> new DepartmentDTO(
                        d.getId(),
                        d.getName(),
                        d.getDepartmentCode(),
                        d.getRegionCode()
                ))
                .collect(Collectors.toList());
    }
}
