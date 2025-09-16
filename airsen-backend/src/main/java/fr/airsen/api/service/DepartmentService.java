package fr.airsen.api.service;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.entity.Department;
import fr.airsen.api.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentDTO(d.getId(), d.getName(), d.getDepartmentCode(), d.getRegionCode()))
                .collect(Collectors.toList());
    }

    public DepartmentDTO getDepartmentById(int id) {
        return departmentRepository.findById(id)
                .map(d -> new DepartmentDTO(d.getId(), d.getName(), d.getDepartmentCode(), d.getRegionCode()))
                .orElse(null);
    }

    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        Department department = new Department(dto.getName(), dto.getDepartmentCode(), dto.getRegionCode(), null);
        Department saved = departmentRepository.save(department);
        return new DepartmentDTO(saved.getId(), saved.getName(), saved.getDepartmentCode(), saved.getRegionCode());
    }

    public DepartmentDTO updateDepartment(int id, DepartmentDTO dto) {
        return departmentRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setDepartmentCode(dto.getDepartmentCode());
                    existing.setRegionCode(dto.getRegionCode());
                    Department updated = departmentRepository.save(existing);
                    return new DepartmentDTO(updated.getId(), updated.getName(), updated.getDepartmentCode(), updated.getRegionCode());
                })
                .orElse(null);
    }

    public void deleteDepartment(int id) {
        departmentRepository.deleteById(id);
    }
}
