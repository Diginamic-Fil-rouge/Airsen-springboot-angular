package fr.airsen.api.repository;

import fr.airsen.api.entity.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegionRepositoryTest {

    @Mock
    private RegionRepository regionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        Region r1 = new Region("Occitanie", "OCC");
        Region r2 = new Region("Nouvelle-Aquitaine", "NAQ");

        when(regionRepository.findAll()).thenReturn(List.of(r1, r2));

        List<Region> regions = regionRepository.findAll();

        assertEquals(2, regions.size());
        verify(regionRepository, times(1)).findAll();
    }

    @Test
    void testFindById() {
        Region region = new Region("Bretagne", "BRE");
        region.setId(1L);

        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));

        Optional<Region> result = regionRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Bretagne", result.get().getName());
        verify(regionRepository, times(1)).findById(1L);
    }

    @Test
    void testFindByRegionCode() {
        Region region = new Region("Île-de-France", "IDF");

        when(regionRepository.findByRegionCode("IDF")).thenReturn(Optional.of(region));

        Optional<Region> result = regionRepository.findByRegionCode("IDF");

        assertTrue(result.isPresent());
        assertEquals("Île-de-France", result.get().getName());
        verify(regionRepository).findByRegionCode("IDF");
    }

    @Test
    void testSave() {
        Region region = new Region("Normandie", "NOR");

        when(regionRepository.save(region)).thenReturn(region);

        Region result = regionRepository.save(region);

        assertNotNull(result);
        assertEquals("Normandie", result.getName());
        verify(regionRepository).save(region);
    }

    @Test
    void testDeleteById() {
        long id = 5L;

        doNothing().when(regionRepository).deleteById(id);

        regionRepository.deleteById(id);

        verify(regionRepository, times(1)).deleteById(id);
    }
}
