package fr.airsen.api.service;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import fr.airsen.api.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void testGetAllDepartments() {
        Region region = new Region();
        region.setRegionCode("01");

        Department dep1 = new Department("Dept1", "D01", "01", region);
        dep1.setId(1L);
        Department dep2 = new Department("Dept2", "D02", "01", region);
        dep2.setId(2L);

        Mockito.when(departmentRepository.findAll()).thenReturn(List.of(dep1, dep2));

        List<DepartmentDTO> result = departmentService.getAllDepartments();
        assertEquals(2, result.size());
        assertEquals("Dept1", result.get(0).getName());
        assertEquals("Dept2", result.get(1).getName());
    }

    @Test
    void testGetDepartmentByIdFound() {
        Region region = new Region();
        region.setRegionCode("01");

        Department dep = new Department("Dept1", "D01", "01", region);
        dep.setId(1L);

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(dep));

        DepartmentDTO result = departmentService.getDepartmentById(1L);
        assertNotNull(result);
        assertEquals("Dept1", result.getName());
        assertEquals("01", result.getRegionCode());
    }

    @Test
    void testGetDepartmentByIdNotFound() {
        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.empty());
        DepartmentDTO result = departmentService.getDepartmentById(1L);
        assertNull(result);
    }
}
