package fr.airsen.api.mapper;

import fr.airsen.api.dto.ForumVoteDTO;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.enums.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ForumVoteMapperTest {

    private ForumVoteMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ForumVoteMapper();
    }

    @Test
    void testToDTO() {
        ForumVote entity = new ForumVote();
        entity.setId(1L);
        entity.setVoteType(VoteType.LIKE);

        ForumVoteDTO dto = mapper.toDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getVoteType()).isEqualTo(entity.getVoteType());
    }

    @Test
    void testToEntity() {
        ForumVoteDTO dto = new ForumVoteDTO();
        dto.setId(2L);
        dto.setVoteType(VoteType.DISLIKE);

        ForumVote entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(dto.getId());
        assertThat(entity.getVoteType()).isEqualTo(dto.getVoteType());
    }

    @Test
    void testToDTOs() {
        ForumVote v1 = new ForumVote();
        v1.setId(1L);
        ForumVote v2 = new ForumVote();
        v2.setId(2L);

        List<ForumVoteDTO> dtos = mapper.toDTOs(Arrays.asList(v1, v2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo(1L);
        assertThat(dtos.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void testToEntities() {
        ForumVoteDTO dto1 = new ForumVoteDTO();
        dto1.setId(1L);
        ForumVoteDTO dto2 = new ForumVoteDTO();
        dto2.setId(2L);

        List<ForumVote> entities = mapper.toEntities(Arrays.asList(dto1, dto2));

        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).getId()).isEqualTo(1L);
        assertThat(entities.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void testEmptyLists() {
        assertThat(mapper.toDTOs(Collections.emptyList())).isEmpty();
        assertThat(mapper.toEntities(Collections.emptyList())).isEmpty();
    }

    @Test
    void testNullFields() {
        ForumVote entity = new ForumVote(); // all fields null
        ForumVoteDTO dto = mapper.toDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getVoteType()).isNull();
    }
}
