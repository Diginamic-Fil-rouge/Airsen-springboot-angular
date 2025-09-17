package fr.airsen.api.service;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.repository.CommuneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommuneService {

    private final CommuneRepository communeRepository;

    public CommuneService(CommuneRepository communeRepository) {
        this.communeRepository = communeRepository;
    }

    public List<CommuneDTO> getAllCommunes() {
        return communeRepository.findAll().stream()
                .map(c -> new CommuneDTO(c.getId(), c.getInseeCode(), c.getName(), c.getDepartmentCode(), c.getRegionCode(), c.getPopulation()))
                .collect(Collectors.toList());
    }

    public CommuneDTO getCommuneById(int id) {
        return communeRepository.findById(id)
                .map(c -> new CommuneDTO(c.getId(), c.getInseeCode(), c.getName(), c.getDepartmentCode(), c.getRegionCode(), c.getPopulation()))
                .orElse(null);
    }

    public CommuneDTO createCommune(CommuneDTO dto) {
        Commune commune = new Commune();
        commune.setInseeCode(dto.getInseeCode());
        commune.setName(dto.getName());
        commune.setDepartmentCode(dto.getDepartmentCode());
        commune.setRegionCode(dto.getRegionCode());
        commune.setPopulation(dto.getPopulation());

        Commune saved = communeRepository.save(commune);
        return new CommuneDTO(saved.getId(), saved.getInseeCode(), saved.getName(), saved.getDepartmentCode(), saved.getRegionCode(), saved.getPopulation());
    }

    public CommuneDTO updateCommune(int id, CommuneDTO dto) {
        return communeRepository.findById(id)
                .map(existing -> {
                    existing.setInseeCode(dto.getInseeCode());
                    existing.setName(dto.getName());
                    existing.setDepartmentCode(dto.getDepartmentCode());
                    existing.setRegionCode(dto.getRegionCode());
                    existing.setPopulation(dto.getPopulation());
                    Commune updated = communeRepository.save(existing);
                    return new CommuneDTO(updated.getId(), updated.getInseeCode(), updated.getName(), updated.getDepartmentCode(), updated.getRegionCode(), updated.getPopulation());
                })
                .orElse(null);
    }

    public void deleteCommune(int id) {
        communeRepository.deleteById(id);
    }
}
