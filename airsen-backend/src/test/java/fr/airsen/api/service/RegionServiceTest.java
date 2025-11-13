package fr.airsen.api.service;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.dto.RegionDTO;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import fr.airsen.api.repository.DepartmentRepository;
import fr.airsen.api.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegionServiceTest {

    private RegionRepository regionRepository;
    private DepartmentRepository departmentRepository;
    private RegionService regionService;

    @BeforeEach
    void setUp() {
        regionRepository = mock(RegionRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        regionService = new RegionService(regionRepository, departmentRepository);
    }

    @Test
    void getAllRegions() {
        // ARRANGE
        Region r1 = new Region("Occitanie", "OCC");
        r1.setId(1L);

        Region r2 = new Region("Bretagne", "BRE");
        r2.setId(2L);

        when(regionRepository.findAll()).thenReturn(List.of(r1, r2));

        // ACT
        List<RegionDTO> result = regionService.getAllRegions();

        // ASSERT
        assertEquals(2, result.size());

        assertEquals("Occitanie", result.get(0).getName());
        assertEquals("OCC", result.get(0).getRegionCode());

        assertEquals("Bretagne", result.get(1).getName());
        assertEquals("BRE", result.get(1).getRegionCode());

        verify(regionRepository, times(1)).findAll();
    }

    @Test
    void getDepartmentsByRegion() {
        // ARRANGE
        Region region = new Region("Occitanie", "OCC");
        region.setId(1L);

        Department d1 = new Department("Hérault", "34", "OCC", region);
        d1.setId(1L);

        Department d2 = new Department("Gard", "30", "OCC", region);
        d2.setId(2L);

        when(departmentRepository.findByRegionId(1L)).thenReturn(List.of(d1, d2));

        // ACT
        List<DepartmentDTO> result = regionService.getDepartmentsByRegion(1L);

        // ASSERT
        assertEquals(2, result.size());

        assertEquals("Hérault", result.get(0).getName());
        assertEquals("34", result.get(0).getDepartmentCode());
        assertEquals("OCC", result.get(0).getRegionCode());

        assertEquals("Gard", result.get(1).getName());
        assertEquals("30", result.get(1).getDepartmentCode());
        assertEquals("OCC", result.get(1).getRegionCode());

        verify(departmentRepository).findByRegionId(1L);
    }
}
