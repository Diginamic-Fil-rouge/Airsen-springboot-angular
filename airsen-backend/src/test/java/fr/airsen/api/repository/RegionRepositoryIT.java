package fr.airsen.api.repository;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegionRepository integration tests")
@Transactional
class RegionRepositoryIT extends AbstractTestContainersTest {

    @Autowired
    private RegionRepository regionRepository;

    private Region occitanie;
    private Region ileDeFrance;

    @BeforeEach
    void setUp() {
        regionRepository.deleteAll();

        occitanie = persistRegion("76", "Occitanie");
        ileDeFrance = persistRegion("11", "Île-de-France");
    }

    @Test
    @DisplayName("save should persist region and it should be retrievable by id")
    void saveAndFindById() {
        Region region = new Region();
        region.setName("Nouvelle-Aquitaine");
        region.setRegionCode("75");

        Region saved = regionRepository.save(region);

        Optional<Region> found = regionRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Nouvelle-Aquitaine");
        assertThat(found.get().getRegionCode()).isEqualTo("75");
    }

    @Test
    @DisplayName("findAll should return all persisted regions")
    void findAll_returnsSavedRegions() {
        List<Region> regions = regionRepository.findAll();
        assertThat(regions).isNotEmpty();
        assertThat(regions).extracting(Region::getName)
                .containsExactlyInAnyOrder("Occitanie", "Île-de-France");
    }

    @Test
    @DisplayName("delete should remove the region")
    void delete_removesRegion() {
        Region region = persistRegion("99", "TestRegion");
        Long id = region.getId();

        assertThat(regionRepository.findById(id)).isPresent();

        regionRepository.deleteById(id);

        assertThat(regionRepository.findById(id)).isNotPresent();
    }

    /* ---------- Helpers ---------- */
    private Region persistRegion(String code, String name) {
        Region region = new Region();
        region.setName(name);
        region.setRegionCode(code);
        return regionRepository.save(region);
    }
}
