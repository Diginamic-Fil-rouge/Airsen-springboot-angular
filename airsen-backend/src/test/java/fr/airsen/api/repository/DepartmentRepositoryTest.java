package fr.airsen.api.repository;

import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DepartmentRepositoryTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialise les mocks
    }

    @Test
    @DisplayName("Doit retourner tous les départements")
    void testFindAll() {
        Department dep1 = new Department("Dept1", "D01", "R01", null);
        Department dep2 = new Department("Dept2", "D02", "R01", null);

        // Simule le comportement du repository
        when(departmentRepository.findAll()).thenReturn(Arrays.asList(dep1, dep2));

        List<Department> result = departmentRepository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).contains(dep1, dep2);

        verify(departmentRepository, times(1)).findAll(); // Vérifie que findAll a été appelé une fois
    }

    @Test
    @DisplayName("Doit trouver un département par ID")
    void testFindById() {
        Department dep = new Department("Dept1", "D01", "R01", null);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dep));

        Optional<Department> result = departmentRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Dept1");

        verify(departmentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Doit trouver les départements par regionId")
    void testFindByRegionId() {
        Region region = new Region();
        region.setId(1L);
        Department dep1 = new Department("Dept1", "D01", "R01", region);
        Department dep2 = new Department("Dept2", "D02", "R01", region);

        when(departmentRepository.findByRegionId(1L)).thenReturn(Arrays.asList(dep1, dep2));

        List<Department> result = departmentRepository.findByRegionId(1L);

        assertThat(result).hasSize(2);
        assertThat(result).contains(dep1, dep2);

        verify(departmentRepository, times(1)).findByRegionId(1L);
    }
}
