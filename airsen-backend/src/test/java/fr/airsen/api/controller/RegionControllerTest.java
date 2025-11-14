package fr.airsen.api.controller;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.dto.RegionDTO;
import fr.airsen.api.service.RegionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RegionService regionService;

    @InjectMocks
    private RegionController regionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(regionController).build();
    }

    @Test
    void getAllRegions() throws Exception {

        RegionDTO r1 = new RegionDTO(1L, "Occitanie", "OCC");
        RegionDTO r2 = new RegionDTO(2L, "Bretagne", "BRE");
        when(regionService.getAllRegions()).thenReturn(List.of(r1, r2));


        mockMvc.perform(get("/regions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("Occitanie"))
                .andExpect(jsonPath("$[0].regionCode").value("OCC"))
                .andExpect(jsonPath("$[1].name").value("Bretagne"))
                .andExpect(jsonPath("$[1].regionCode").value("BRE"));

        verify(regionService, times(1)).getAllRegions();
    }

    @Test
    void getDepartmentsByRegion() throws Exception {
        // ARRANGE
        DepartmentDTO d1 = new DepartmentDTO(1L, "Hérault", "34", "OCC");
        DepartmentDTO d2 = new DepartmentDTO(2L, "Gard", "30", "OCC");

        when(regionService.getDepartmentsByRegion(1L)).thenReturn(List.of(d1, d2));

        // ACT & ASSERT
        mockMvc.perform(get("/regions/1/departments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("Hérault"))
                .andExpect(jsonPath("$[0].departmentCode").value("34"))
                .andExpect(jsonPath("$[0].regionCode").value("OCC"))
                .andExpect(jsonPath("$[1].name").value("Gard"))
                .andExpect(jsonPath("$[1].departmentCode").value("30"))
                .andExpect(jsonPath("$[1].regionCode").value("OCC"));

        verify(regionService, times(1)).getDepartmentsByRegion(1L);
    }
}
