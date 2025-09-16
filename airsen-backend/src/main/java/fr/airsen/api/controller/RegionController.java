package fr.airsen.api.controller;

import fr.airsen.api.dto.RegionDTO;
import fr.airsen.api.service.RegionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public List<RegionDTO> getAllRegions() {
        return regionService.getAllRegions();
    }

    @GetMapping("/{id}")
    public RegionDTO getRegionById(@PathVariable int id) {
        return regionService.getRegionById(id);
    }

    @PostMapping
    public RegionDTO createRegion(@RequestBody RegionDTO dto) {
        return regionService.createRegion(dto);
    }

    @PutMapping("/{id}")
    public RegionDTO updateRegion(@PathVariable int id, @RequestBody RegionDTO dto) {
        return regionService.updateRegion(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteRegion(@PathVariable int id) {
        regionService.deleteRegion(id);
    }
}

