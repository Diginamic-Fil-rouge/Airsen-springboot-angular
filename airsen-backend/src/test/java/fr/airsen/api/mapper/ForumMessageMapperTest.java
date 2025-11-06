package fr.airsen.api.mapper;

import fr.airsen.api.dto.ForumMessageDTO;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ForumMessageMapperTest {

    private UserRepository userRepository;
    private ForumMessageMapper mapper;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mapper = new ForumMessageMapper(userRepository);
    }

    @Test
    void testToDTO() {
        User author = new User();
        author.setId(1L);
        author.setFirstName("john");
        author.setLastName("doe");

        ForumMessage entity = new ForumMessage();
        entity.setId(10L);
        entity.setContent("Hello world");
        entity.setCreatedDate(LocalDateTime.now());
        entity.setAuthor(author);

        ForumMessageDTO dto = mapper.toDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getContent()).isEqualTo(entity.getContent());
        assertThat(dto.getCreatedDate()).isEqualTo(entity.getCreatedDate());
        assertThat(dto.getAuthor()).isNotNull();
        assertThat(dto.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(dto.getAuthor().getFullName()).isEqualTo(author.getFullName());
    }

    @Test
    void testToEntityWithExistingAuthor() {
        User author = new User();
        author.setId(1L);
        author.setFirstName("john");
        author.setLastName("doe");

        UserDTO authorDTO = new UserDTO();
        authorDTO.setId(1L);

        ForumMessageDTO dto = new ForumMessageDTO();
        dto.setId(10L);
        dto.setContent("Hello world");
        dto.setCreatedDate(LocalDateTime.now());
        dto.setAuthor(authorDTO);

        // Mock repository
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        ForumMessage entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(dto.getId());
        assertThat(entity.getContent()).isEqualTo(dto.getContent());
        assertThat(entity.getCreatedDate()).isEqualTo(dto.getCreatedDate());
        assertThat(entity.getAuthor()).isNotNull();
        assertThat(entity.getAuthor().getId()).isEqualTo(author.getId());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testToEntityWithNonExistingAuthor() {
        UserDTO authorDTO = new UserDTO();
        authorDTO.setId(99L);

        ForumMessageDTO dto = new ForumMessageDTO();
        dto.setId(10L);
        dto.setContent("Hello world");
        dto.setCreatedDate(LocalDateTime.now());
        dto.setAuthor(authorDTO);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ForumMessage entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getAuthor()).isNull();

        verify(userRepository, times(1)).findById(99L);
    }

    @Test
    void testToEntityWithNullAuthor() {
        ForumMessageDTO dto = new ForumMessageDTO();
        dto.setId(10L);
        dto.setContent("Hello world");
        dto.setCreatedDate(LocalDateTime.now());
        dto.setAuthor(null);

        ForumMessage entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getAuthor()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void testToDTOs() {
        ForumMessage m1 = new ForumMessage();
        m1.setId(1L);
        ForumMessage m2 = new ForumMessage();
        m2.setId(2L);

        List<ForumMessageDTO> dtos = mapper.toDTOs(Arrays.asList(m1, m2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo(1L);
        assertThat(dtos.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void testToEntities() {
        ForumMessageDTO dto1 = new ForumMessageDTO();
        dto1.setId(1L);
        ForumMessageDTO dto2 = new ForumMessageDTO();
        dto2.setId(2L);

        List<ForumMessage> entities = mapper.toEntities(Arrays.asList(dto1, dto2));

        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).getId()).isEqualTo(1L);
        assertThat(entities.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void testEmptyLists() {
        assertThat(mapper.toDTOs(Collections.emptyList())).isEmpty();
        assertThat(mapper.toEntities(Collections.emptyList())).isEmpty();
    }
}
