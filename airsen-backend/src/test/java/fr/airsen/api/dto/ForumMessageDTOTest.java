package fr.airsen.api.dto;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumMessageDTOTest {

    private ForumCategory category;
    private ForumMessage forumMessage;
    private User user;
    private ForumThread thread;

    @BeforeEach
    void setUp() {
        category = mock(ForumCategory.class);
        forumMessage = mock(ForumMessage.class);
        user = mock(User.class);
        thread = mock(ForumThread.class);

        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("jane.doe@example.com");
        when(user.getFirstName()).thenReturn("Jane");
        when(user.getLastName()).thenReturn("Doe");
        when(user.getAddress()).thenReturn("123 Street");
        when(user.getTelephone()).thenReturn("123456789");
        when(user.getBio()).thenReturn("Bio");
        when(user.getRole()).thenReturn(UserRole.USER);

        when(thread.getCategory()).thenReturn(category);

        when(forumMessage.getId()).thenReturn(10L);
        when(forumMessage.getAuthor()).thenReturn(user);
        when(forumMessage.getThread()).thenReturn(thread);
        when(forumMessage.getContent()).thenReturn("Test message");
        when(forumMessage.getCreatedDate()).thenReturn(LocalDateTime.of(2025, 1, 4, 10, 0));
    }

    @Test
    void testDefaultConstructor() {
        ForumMessageDTO dto = new ForumMessageDTO();

        assertNotNull(dto);
        assertEquals(0L, dto.getId());
        assertNull(dto.getAuthor());
        assertNull(dto.getThread());
        assertNull(dto.getContent());
        assertNull(dto.getCreatedDate());
    }

    @Test
    void testConstructorWithThreadTrue() {
        ForumMessageDTO dto = new ForumMessageDTO(forumMessage, true);

        assertEquals(10L, dto.getId());
        assertNotNull(dto.getAuthor());
        assertEquals("jane.doe@example.com", dto.getAuthor().getEmail());
        assertEquals("Test message", dto.getContent());
        assertEquals(LocalDateTime.of(2025, 1, 4, 10, 0), dto.getCreatedDate());
        assertNotNull(dto.getThread());
    }

    @Test
    void testConstructorWithThreadFalse() {
        ForumMessageDTO dto = new ForumMessageDTO(forumMessage, false);

        assertEquals(10L, dto.getId());
        assertNotNull(dto.getAuthor());
        assertEquals("Jane", dto.getAuthor().getFirstName());
        assertEquals("Test message", dto.getContent());
        assertEquals(LocalDateTime.of(2025, 1, 4, 10, 0), dto.getCreatedDate());
        assertNull(dto.getThread());
    }

    @Test
    void testGettersAndSetters() {
        ForumMessageDTO dto = new ForumMessageDTO();

        UserDTO authorDTO = new UserDTO(2L, "john@example.com", "John", "Smith", "456 Road", "987654321", "Bio", UserRole.ADMIN);
        ForumThreadDTO threadDTO = mock(ForumThreadDTO.class);
        LocalDateTime now = LocalDateTime.now();

        dto.setId(5L);
        dto.setAuthor(authorDTO);
        dto.setThread(threadDTO);
        dto.setContent("Hello world");
        dto.setCreatedDate(now);

        assertEquals(5L, dto.getId());
        assertEquals(authorDTO, dto.getAuthor());
        assertEquals(threadDTO, dto.getThread());
        assertEquals("Hello world", dto.getContent());
        assertEquals(now, dto.getCreatedDate());
    }
}
