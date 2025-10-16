package fr.airsen.api.service;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.dto.RegionDTO;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import fr.airsen.api.repository.DepartmentRepository;
import fr.airsen.api.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegionService {

    private final RegionRepository regionRepository;
    private final DepartmentRepository departmentRepository;

    public RegionService(RegionRepository regionRepository, DepartmentRepository departmentRepository) {
        this.regionRepository = regionRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<RegionDTO> getAllRegions() {
        return regionRepository.findAll().stream()
                .map(r -> new RegionDTO(
                        r.getId(),
                        r.getName(),
                        r.getRegionCode()
                ))
                .collect(Collectors.toList());
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
