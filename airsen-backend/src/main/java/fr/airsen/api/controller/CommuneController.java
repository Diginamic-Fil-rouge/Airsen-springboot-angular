package fr.airsen.api.controller;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.service.CommuneService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments/{departmentId}/communes")
@Tag(name = "Communes", description = "Communes endpoints")
public class CommuneController {

    private final CommuneService communeService;

    public CommuneController(CommuneService communeService) {
        this.communeService = communeService;
    }

    /**
     * GET /departments/{departmentId}/communes
     * Liste les communes d'un département
     */
    @GetMapping
    public List<CommuneDTO> getCommunesByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return communeService.getCommunesByDepartment(departmentId, page, size, search);
    }
}
