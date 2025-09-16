package fr.airsen.api.controller;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.service.CommuneService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/communes")
public class CommuneController {

    private final CommuneService communeService;

    public CommuneController(CommuneService communeService) {
        this.communeService = communeService;
    }

    @GetMapping
    public List<CommuneDTO> getAllCommunes() {
        return communeService.getAllCommunes();
    }

    @GetMapping("/{id}")
    public CommuneDTO getCommuneById(@PathVariable int id) {
        return communeService.getCommuneById(id);
    }

    @PostMapping
    public CommuneDTO createCommune(@RequestBody CommuneDTO dto) {
        return communeService.createCommune(dto);
    }

    @PutMapping("/{id}")
    public CommuneDTO updateCommune(@PathVariable int id, @RequestBody CommuneDTO dto) {
        return communeService.updateCommune(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteCommune(@PathVariable int id) {
        communeService.deleteCommune(id);
    }
}
