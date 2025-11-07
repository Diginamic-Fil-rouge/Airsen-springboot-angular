package fr.airsen.api.mapper;

import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ForumThreadMapperTest {

    private ForumThreadMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ForumThreadMapper();
    }

    @Test
    void testToDTO() {
        User author = new User();
        author.setId(1L);
        ForumVote vote = new ForumVote();
        vote.setVoteType(VoteType.LIKE);
        vote.setUser(author);
        ForumCategory category = new ForumCategory();
        category.setId(1L);
        ForumThread entity = new ForumThread();
        vote.setThread(entity);
        entity.setId(1L);
        entity.setTitle("Thread title");
        entity.setContent("Thread content");
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastMessageDate(LocalDateTime.now().plusHours(1));
        entity.setViewCount(100);
        entity.setPinned(true);
        entity.setClosed(false);
        entity.setVotes(new ArrayList<>());
        entity.getVotes().add(vote);
        entity.setCategory(category);
        entity.setAuthor(author);

        ForumThreadDTO dto = mapper.toDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getTitle()).isEqualTo(entity.getTitle());
        assertThat(dto.getContent()).isEqualTo(entity.getContent());
        assertThat(dto.getCreatedDate()).isEqualTo(entity.getCreatedDate());
        assertThat(dto.getLastMessageDate()).isEqualTo(entity.getLastMessageDate());
        assertThat(dto.getViewCount()).isEqualTo(entity.getViewCount());
        assertThat(dto.isPinned()).isEqualTo(entity.isPinned());
        assertThat(dto.isClosed()).isEqualTo(entity.isClosed());
        assertThat(dto.getLikeCount()).isEqualTo(entity.getVotesValue());
    }

    @Test
    void testToEntity() {
        ForumThreadDTO dto = new ForumThreadDTO();
        dto.setId(2L);
        dto.setTitle("Another thread");
        dto.setContent("Another content");
        dto.setCreatedDate(LocalDateTime.now());
        dto.setLastMessageDate(LocalDateTime.now().plusHours(2));
        dto.setViewCount(50);
        dto.setPinned(false);
        dto.setClosed(true);
        dto.setLikeCount(10);

        ForumThread entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(dto.getId());
        assertThat(entity.getTitle()).isEqualTo(dto.getTitle());
        assertThat(entity.getContent()).isEqualTo(dto.getContent());
        assertThat(entity.getCreatedDate()).isEqualTo(dto.getCreatedDate());
        assertThat(entity.getLastMessageDate()).isEqualTo(dto.getLastMessageDate());
        assertThat(entity.getViewCount()).isEqualTo(dto.getViewCount());
        assertThat(entity.isPinned()).isEqualTo(dto.isPinned());
        assertThat(entity.isClosed()).isEqualTo(dto.isClosed());
        assertThat(entity.getLikeCount()).isEqualTo(dto.getLikeCount());
    }

    @Test
    void testToDTOs() {
        User author = new User();
        author.setId(1L);
        ForumCategory category = new ForumCategory();
        category.setId(1L);
        ForumThread t1 = new ForumThread();
        t1.setId(1L);
        t1.setCategory(category);
        ForumThread t2 = new ForumThread();
        t2.setId(2L);
        t2.setCategory(category);

        List<ForumThreadDTO> dtos = mapper.toDTOs(Arrays.asList(t1, t2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo(1L);
        assertThat(dtos.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void testToEntities() {
        ForumThreadDTO dto1 = new ForumThreadDTO();
        dto1.setId(1L);
        ForumThreadDTO dto2 = new ForumThreadDTO();
        dto2.setId(2L);

        List<ForumThread> entities = mapper.toEntities(Arrays.asList(dto1, dto2));

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
        ForumCategory category = new ForumCategory();
        category.setId(1L);
        ForumThread entity = new ForumThread(); // all fields null
        entity.setCategory(category);
        ForumThreadDTO dto = mapper.toDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(0L);
        assertThat(dto.getTitle()).isNull();
        assertThat(dto.getContent()).isNull();
        assertThat(dto.getCreatedDate()).isNull();
        assertThat(dto.getLastMessageDate()).isNull();
        assertThat(dto.getViewCount()).isNull(); // primitive defaults
        assertThat(dto.isPinned()).isFalse(); // primitive defaults
        assertThat(dto.isClosed()).isFalse(); // primitive defaults
        assertThat(dto.getLikeCount()).isEqualTo(0);
    }
}
