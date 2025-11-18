package fr.airsen.api.dto;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.Region;
import fr.airsen.api.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegionDTO integration tests")
@Transactional
class RegionDTOIT extends AbstractTestContainersTest {

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
    @DisplayName("RegionDTO should correctly map from Region entity")
    void regionDTO_mapsCorrectly() {
        // GIVEN
        Optional<Region> regionOpt = regionRepository.findById(occitanie.getId());
        assertThat(regionOpt).isPresent();
        Region region = regionOpt.get();

        // WHEN
        RegionDTO dto = new RegionDTO(
                region.getId(),
                region.getName(),
                region.getRegionCode()
        );

        // THEN
        assertThat(dto.getId()).isEqualTo(region.getId());
        assertThat(dto.getName()).isEqualTo(region.getName());
        assertThat(dto.getRegionCode()).isEqualTo(region.getRegionCode());
    }

    @Test
    @DisplayName("RegionDTO should reflect updated Region data")
    void regionDTO_reflectsUpdatedEntity() {
        // GIVEN
        occitanie.setName("Occitanie Modifiée");
        regionRepository.save(occitanie);

        RegionDTO dto = new RegionDTO(
                occitanie.getId(),
                occitanie.getName(),
                occitanie.getRegionCode()
        );

        // THEN
        assertThat(dto.getId()).isEqualTo(occitanie.getId());
        assertThat(dto.getName()).isEqualTo("Occitanie Modifiée");
        assertThat(dto.getRegionCode()).isEqualTo("76");
    }

    @Test
    @DisplayName("findAll should return all persisted regions for DTO conversion")
    void findAll_returnsSavedRegionsForDTO() {
        List<Region> regions = regionRepository.findAll();

        assertThat(regions).hasSize(2);
        assertThat(regions).extracting(Region::getName)
                .containsExactlyInAnyOrder("Occitanie", "Île-de-France");

        // Convert to DTOs
        List<RegionDTO> dtos = regions.stream()
                .map(r -> new RegionDTO(r.getId(), r.getName(), r.getRegionCode()))
                .toList();

        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting(RegionDTO::getRegionCode)
                .containsExactlyInAnyOrder("76", "11");
    }

    /* ---------- Helpers ---------- */
    private Region persistRegion(String code, String name) {
        Region region = new Region();
        region.setName(name);
        region.setRegionCode(code);
        return regionRepository.save(region);
    }
}
