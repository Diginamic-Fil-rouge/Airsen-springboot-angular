package fr.airsen.api.service;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.Department;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommuneService {

    private final CommuneRepository communeRepository;
    private final DepartmentRepository departmentRepository;

    public CommuneService(CommuneRepository communeRepository, DepartmentRepository departmentRepository) {
        this.communeRepository = communeRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<CommuneDTO> getAllCommunes() {
        return communeRepository.findAll().stream()
                .map(c -> new CommuneDTO(
                        c.getId(),
                        c.getInseeCode(),
                        c.getName(),
                        c.getDepartmentCode(),
                        c.getRegionCode(),
                        c.getPopulation()
                ))
                .collect(Collectors.toList());
    }

    public CommuneDTO getCommuneById(int id) {
        return communeRepository.findById(id)
                .map(c -> new CommuneDTO(
                        c.getId(),
                        c.getInseeCode(),
                        c.getName(),
                        c.getDepartmentCode(),
                        c.getRegionCode(),
                        c.getPopulation()
                ))
                .orElse(null);
    }

    public CommuneDTO createCommune(CommuneDTO dto) {
        Commune commune = new Commune();
        commune.setInseeCode(dto.getInseeCode());
        commune.setName(dto.getName());
        commune.setRegionCode(dto.getRegionCode());
        commune.setPopulation(dto.getPopulation());

        // Convert String departmentCode to Department entity
        Optional<Department> departmentOpt = departmentRepository.findByDepartmentCode(Integer.parseInt(dto.getDepartmentCode()));
        if (departmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid department code: " + dto.getDepartmentCode());
        }
        Department department = departmentOpt.get();
        commune.setDepartment(department);
        commune.setDepartmentCode(dto.getDepartmentCode()); // keep the string in DTO

        Commune saved = communeRepository.save(commune);
        return new CommuneDTO(
                saved.getId(),
                saved.getInseeCode(),
                saved.getName(),
                saved.getDepartmentCode(),
                saved.getRegionCode(),
                saved.getPopulation()
        );
    }

    public CommuneDTO updateCommune(int id, CommuneDTO dto) {
        return communeRepository.findById(id)
                .map(existing -> {
                    existing.setInseeCode(dto.getInseeCode());
                    existing.setName(dto.getName());
                    existing.setRegionCode(dto.getRegionCode());
                    existing.setPopulation(dto.getPopulation());

                    // Convert String departmentCode to Department entity
                    Optional<Department> departmentOpt = departmentRepository.findByDepartmentCode(Integer.parseInt(dto.getDepartmentCode()));
                    if (departmentOpt.isEmpty()) {
                        throw new IllegalArgumentException("Invalid department code: " + dto.getDepartmentCode());
                    }
                    Department department = departmentOpt.get();
                    existing.setDepartment(department);
                    existing.setDepartmentCode(dto.getDepartmentCode());

                    Commune updated = communeRepository.save(existing);
                    return new CommuneDTO(
                            updated.getId(),
                            updated.getInseeCode(),
                            updated.getName(),
                            updated.getDepartmentCode(),
                            updated.getRegionCode(),
                            updated.getPopulation()
                    );
                })
                .orElse(null);
    }

    public void deleteCommune(int id) {
        communeRepository.deleteById(id);
    }
}
