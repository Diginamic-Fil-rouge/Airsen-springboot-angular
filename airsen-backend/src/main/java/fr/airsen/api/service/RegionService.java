package fr.airsen.api.service;

import fr.airsen.api.dto.RegionDTO;
import fr.airsen.api.entity.Region;
import fr.airsen.api.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    public List<RegionDTO> getAllRegions() {
        return regionRepository.findAll().stream()
                .map(r -> new RegionDTO(r.getId(), r.getName(), r.getRegionCode()))
                .collect(Collectors.toList());
    }

    public RegionDTO getRegionById(int id) {
        return regionRepository.findById(id)
                .map(r -> new RegionDTO(r.getId(), r.getName(), r.getRegionCode()))
                .orElse(null);
    }

    public RegionDTO createRegion(RegionDTO dto) {
        Region region = new Region(dto.getName(), dto.getRegionCode());
        Region saved = regionRepository.save(region);
        return new RegionDTO(saved.getId(), saved.getName(), saved.getRegionCode());
    }

    public void deleteRegion(int id) {
        regionRepository.deleteById(id);
    }

    public RegionDTO updateRegion(int id, RegionDTO dto) {
        return regionRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setRegionCode(dto.getRegionCode());
                    Region updated = regionRepository.save(existing);
                    return new RegionDTO(updated.getId(), updated.getName(), updated.getRegionCode());
                })
                .orElse(null);
    }

}
