package fr.airsen.api.controller;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.dto.RegionDTO;
import fr.airsen.api.service.RegionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    /**
     * GET /regions
     * Liste toutes les régions
     */
    @GetMapping
    public List<RegionDTO> getAllRegions() {
        return regionService.getAllRegions();
    }

    /**
     * GET /regions/{regionId}/departments
     * Liste les départements d’une région donnée
     */
    @GetMapping("/{regionId}/departments")
    public List<DepartmentDTO> getDepartmentsByRegion(@PathVariable int regionId) {
        return regionService.getDepartmentsByRegion(regionId);
    }
}
