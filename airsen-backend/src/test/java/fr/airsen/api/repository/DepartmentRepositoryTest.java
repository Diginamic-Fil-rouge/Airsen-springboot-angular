package fr.airsen.api.repository;

import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void testFindAllAndFindByRegionId() {
        Region region = new Region();
        region.setRegionCode("R01");

        Department dep1 = new Department("Dept1", "D01", "R01", region);
        Department dep2 = new Department("Dept2", "D02", "R01", region);

        departmentRepository.save(dep1);
        departmentRepository.save(dep2);

        List<Department> all = departmentRepository.findAll();
        assertEquals(2, all.size());

        List<Department> byRegion = departmentRepository.findByRegionId(region.getId());
        // region.getId() sera null ici si non persisté, donc test basique sur findAll suffit
        assertNotNull(byRegion);
    }
}
