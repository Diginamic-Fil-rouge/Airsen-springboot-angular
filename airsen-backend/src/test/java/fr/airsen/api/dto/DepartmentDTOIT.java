package fr.airsen.api.dto;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import fr.airsen.api.repository.DepartmentRepository;
import fr.airsen.api.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DepartmentDTO integration tests")
@Transactional
class DepartmentDTOIT extends AbstractTestContainersTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegionRepository regionRepository;

    private Region occitanie;
    private Department herault;

    @BeforeEach
    void setUp() {
        regionRepository.deleteAll();
        departmentRepository.deleteAll();

        occitanie = persistRegion("76", "Occitanie");
        herault = persistDepartment("34", "Hérault", occitanie);
    }

    @Test
    @DisplayName("DepartmentDTO should correctly map from Department entity")
    void departmentDTO_mapsCorrectly() {
        // GIVEN
        Optional<Department> deptOpt = departmentRepository.findById(herault.getId());
        assertThat(deptOpt).isPresent();
        Department dept = deptOpt.get();

        // WHEN
        DepartmentDTO dto = new DepartmentDTO(
                dept.getId(),
                dept.getName(),
                dept.getDepartmentCode(),
                dept.getRegionCode()
        );

        // THEN
        assertThat(dto.getId()).isEqualTo(dept.getId());
        assertThat(dto.getName()).isEqualTo(dept.getName());
        assertThat(dto.getDepartmentCode()).isEqualTo(dept.getDepartmentCode());
        assertThat(dto.getRegionCode()).isEqualTo(dept.getRegionCode());
    }

    @Test
    @DisplayName("DepartmentDTO should reflect updated Department data")
    void departmentDTO_reflectsUpdatedEntity() {
        // GIVEN
        herault.setName("Hérault Modifié");
        departmentRepository.save(herault);

        DepartmentDTO dto = new DepartmentDTO(
                herault.getId(),
                herault.getName(),
                herault.getDepartmentCode(),
                herault.getRegionCode()
        );

        // THEN
        assertThat(dto.getId()).isEqualTo(herault.getId());
        assertThat(dto.getName()).isEqualTo("Hérault Modifié");
        assertThat(dto.getDepartmentCode()).isEqualTo("34");
        assertThat(dto.getRegionCode()).isEqualTo("76");
    }

    /* ---------- Helpers ---------- */
    private Region persistRegion(String code, String name) {
        Region region = new Region();
        region.setName(name);
        region.setRegionCode(code);
        return regionRepository.save(region);
    }

    private Department persistDepartment(String code, String name, Region region) {
        Department department = new Department();
        department.setName(name);
        department.setDepartmentCode(code);
        department.setRegion(region);
        department.setRegionCode(region.getRegionCode());
        return departmentRepository.save(department);
    }
}
