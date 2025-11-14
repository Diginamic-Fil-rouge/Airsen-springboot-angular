package fr.airsen.api.mapper;

import fr.airsen.api.dto.ForumCategoryDTO;
import fr.airsen.api.entity.ForumCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ForumCategoryMapperTest {

    private ForumCategoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ForumCategoryMapper();
    }

    @Test
    void testToDTO() {
        ForumCategory entity = new ForumCategory();
        entity.setId(1L);
        entity.setName("General");
        entity.setDescription("General discussion");
        entity.setColor("blue");

        ForumCategoryDTO dto = mapper.toDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getName()).isEqualTo(entity.getName());
        assertThat(dto.getDescription()).isEqualTo(entity.getDescription());
        assertThat(dto.getColor()).isEqualTo(entity.getColor());
    }

    @Test
    void testToEntity() {
        ForumCategoryDTO dto = new ForumCategoryDTO();
        dto.setId(2L);
        dto.setName("Tech");
        dto.setDescription("Technology discussions");
        dto.setColor("green");

        ForumCategory entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(dto.getId());
        assertThat(entity.getName()).isEqualTo(dto.getName());
        assertThat(entity.getDescription()).isEqualTo(dto.getDescription());
        assertThat(entity.getColor()).isEqualTo(dto.getColor());
    }

    @Test
    void testToDTOs() {
        ForumCategory entity1 = new ForumCategory();
        entity1.setId(1L);
        entity1.setName("General");

        ForumCategory entity2 = new ForumCategory();
        entity2.setId(2L);
        entity2.setName("Tech");

        List<ForumCategory> entities = Arrays.asList(entity1, entity2);

        List<ForumCategoryDTO> dtos = mapper.toDTOs(entities);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo(entity1.getId());
        assertThat(dtos.get(1).getId()).isEqualTo(entity2.getId());
    }

    @Test
    void testToEntities() {
        ForumCategoryDTO dto1 = new ForumCategoryDTO();
        dto1.setId(1L);
        dto1.setName("General");

        ForumCategoryDTO dto2 = new ForumCategoryDTO();
        dto2.setId(2L);
        dto2.setName("Tech");

        List<ForumCategoryDTO> dtos = Arrays.asList(dto1, dto2);

        List<ForumCategory> entities = mapper.toEntities(dtos);

        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).getId()).isEqualTo(dto1.getId());
        assertThat(entities.get(1).getId()).isEqualTo(dto2.getId());
    }

    @Test
    void testEmptyLists() {
        assertThat(mapper.toDTOs(Collections.emptyList())).isEmpty();
        assertThat(mapper.toEntities(Collections.emptyList())).isEmpty();
    }

    @Test
    void testNullFields() {
        ForumCategory entity = new ForumCategory(); // all fields null
        ForumCategoryDTO dto = mapper.toDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(0L);
        assertThat(dto.getName()).isNull();
        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getColor()).isNull();
    }
}
