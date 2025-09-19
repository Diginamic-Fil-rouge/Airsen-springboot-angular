package fr.airsen.api.service;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.repository.CommuneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommuneService {

    private final CommuneRepository communeRepository;

    public CommuneService(CommuneRepository communeRepository) {
        this.communeRepository = communeRepository;
    }

    public List<CommuneDTO> getCommunesByDepartment(Long departmentId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        List<Commune> communes;

        if (search != null && !search.isEmpty()) {
            communes = communeRepository.findByDepartmentIdAndNameContainingIgnoreCase(departmentId, search, pageable);
        } else {
            communes = communeRepository.findByDepartmentIdAsList(departmentId, pageable);
        }

        return communes.stream()
                .map(c -> new CommuneDTO(
                        c.getId(),
                        c.getInseeCode(),
                        c.getName(),
                        String.valueOf(c.getDepartment().getDepartmentCode()),
                        c.getRegionCode(),
                        c.getPopulation()
                ))
                .collect(Collectors.toList());
    }

    public List<CommuneDTO> searchCommunes(String query, int limit) {
        Page<Commune> communePage = communeRepository.findByNameContainingIgnoreCase(query, PageRequest.of(0, limit));
        List<Commune> communes = communePage.getContent();
        return communes.stream()
                .map(c -> new CommuneDTO(
                        c.getId(),
                        c.getInseeCode(),
                        c.getName(),
                        String.valueOf(c.getDepartment().getDepartmentCode()),
                        c.getRegionCode(),
                        c.getPopulation()
                ))
                .collect(Collectors.toList());
    }
}
