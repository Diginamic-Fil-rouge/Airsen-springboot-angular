package fr.airsen.api.repository;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests pour DepartmentRepository.
 * Hérite d'AbstractTestContainersTest (configuration TestContainers / contexte partagé).
 * Chaque test est transactionnel (rollback automatique).
 */
@DisplayName("DepartmentRepository integration tests")
@Transactional
class DepartmentRepositoryIT extends AbstractTestContainersTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegionRepository regionRepository;

    private Region regionOccitanie;
    private Region regionIleDeFrance;

    @BeforeEach
    void setUp() {
        // nettoyage pour garantir que les tests sont indépendants si nécessaire
        departmentRepository.deleteAll();
        regionRepository.deleteAll();

        regionOccitanie = persistRegion("76", "Occitanie");
        persistDepartment("34", "Hérault", regionOccitanie);

        regionIleDeFrance = persistRegion("11", "Île-de-France");
        persistDepartment("75", "Paris", regionIleDeFrance);
    }

    @Test
    @DisplayName("save should persist department and it should be retrievable by id")
    void saveAndFindById() {
        // arrange
        Department dept = new Department();
        dept.setName("Test Dept");
        dept.setDepartmentCode("99");
        dept.setRegion(regionOccitanie);
        dept.setRegionCode(regionOccitanie.getRegionCode());

        // act
        Department saved = departmentRepository.save(dept);
        Optional<Department> found = departmentRepository.findById(saved.getId());

        // assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Dept");
        assertThat(found.get().getDepartmentCode()).isEqualTo("99");
        assertThat(found.get().getRegion()).isNotNull();
        assertThat(found.get().getRegion().getName()).isEqualTo("Occitanie");
    }

    @Test
    @DisplayName("findAll should return all persisted departments")
    void findAll_returnsSavedDepartments() {
        List<Department> departments = departmentRepository.findAll();

        // we expect at least the two departments created in setUp
        assertThat(departments).isNotEmpty();
        assertThat(departments).anySatisfy(dept -> {
            assertThat(dept.getDepartmentCode()).isEqualTo("34");
            assertThat(dept.getName()).isEqualTo("Hérault");
        }).anySatisfy(dept -> {
            assertThat(dept.getDepartmentCode()).isEqualTo("75");
            assertThat(dept.getName()).isEqualTo("Paris");
        });
    }

    @Test
    @DisplayName("delete should remove the department")
    void delete_removesDepartment() {
        Department toDelete = new Department();
        toDelete.setName("To Delete");
        toDelete.setDepartmentCode("98");
        toDelete.setRegion(regionIleDeFrance);
        toDelete.setRegionCode(regionIleDeFrance.getRegionCode());

        Department saved = departmentRepository.save(toDelete);
        Long id = saved.getId();

        // ensure present
        assertThat(departmentRepository.findById(id)).isPresent();

        // delete and assert removed
        departmentRepository.deleteById(id);
        assertThat(departmentRepository.findById(id)).isNotPresent();
    }

    @Test
    @DisplayName("search by department code via findAll + filter should locate the department")
    void findByDepartmentCode_viaFilterOnFindAll() {
        // This test doesn't rely on a custom repository method; it filters the findAll result.
        String code = "34"; // one created in setUp

        List<Department> departments = departmentRepository.findAll();
        Optional<Department> maybe = departments.stream()
                .filter(d -> code.equals(d.getDepartmentCode()))
                .findFirst();

        assertThat(maybe).isPresent();
        Department dept = maybe.get();
        assertThat(dept.getName()).isEqualTo("Hérault");
        assertThat(dept.getRegion()).isNotNull();
        assertThat(dept.getRegion().getName()).isEqualTo("Occitanie");
    }

    @Test
    @DisplayName("departments can be queried by region association via findAll + filter")
    void findByRegionAssociation_viaFilterOnFindAll() {
        String regionCode = regionIleDeFrance.getRegionCode();

        List<Department> departments = departmentRepository.findAll();
        List<Department> filtered = departments.stream()
                .filter(d -> regionCode.equals(d.getRegionCode()))
                .toList();

        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allSatisfy(d -> assertThat(d.getRegion().getRegionCode()).isEqualTo(regionCode));
    }

    /* --------------------------
       Helpers
       -------------------------- */

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
