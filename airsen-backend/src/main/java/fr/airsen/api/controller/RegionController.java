package fr.airsen.api.controller;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.dto.RegionDTO;
import fr.airsen.api.service.RegionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/regions")
@Tag(name = "Regions", description = "Regions endpoints")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }


    @GetMapping
    public List<RegionDTO> getAllRegions() {
        return regionService.getAllRegions();
    }

    @GetMapping("/{regionId}/departments")
    public List<DepartmentDTO> getDepartmentsByRegion(@PathVariable Long regionId) {
        return regionService.getDepartmentsByRegion(regionId);
    }
}
