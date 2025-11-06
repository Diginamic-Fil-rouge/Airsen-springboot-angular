package fr.airsen.api.dto;

import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumCategoryDTOTest {

    private ForumCategory forumCategory;
    private ForumThread forumThread;

    @BeforeEach
    void setUp() {
        forumCategory = mock(ForumCategory.class);
        forumThread = mock(ForumThread.class);

        when(forumCategory.getId()).thenReturn(1L);
        when(forumCategory.getName()).thenReturn("General Discussion");
        when(forumCategory.getDescription()).thenReturn("Main forum area");
        when(forumCategory.getColor()).thenReturn("#00FF00");
        when(forumCategory.getThreads()).thenReturn(List.of(forumThread));
    }

    @Test
    void testDefaultConstructor() {
        ForumCategoryDTO dto = new ForumCategoryDTO();

        assertNotNull(dto);
        assertEquals(0L, dto.getId());
        assertNull(dto.getName());
        assertNull(dto.getDescription());
        assertNull(dto.getColor());
        assertNull(dto.getThreads());
    }

    @Test
    void testConstructorWithThreadsTrue() {
        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, true);

        assertEquals(1L, dto.getId());
        assertEquals("General Discussion", dto.getName());
        assertEquals("Main forum area", dto.getDescription());
        assertEquals("#00FF00", dto.getColor());
        assertNotNull(dto.getThreads());
        assertEquals(1, dto.getThreads().size());
    }

    @Test
    void testConstructorWithThreadsFalse() {
        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, false);

        assertEquals(1L, dto.getId());
        assertEquals("General Discussion", dto.getName());
        assertEquals("Main forum area", dto.getDescription());
        assertEquals("#00FF00", dto.getColor());
        assertNull(dto.getThreads());
    }

    @Test
    void testConstructorHandlesNullThreadsGracefully() {
        when(forumCategory.getThreads()).thenReturn(null);

        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, true);

        assertNotNull(dto.getThreads());
        assertTrue(dto.getThreads().isEmpty());
    }

    @Test
    void testConstructorHandlesEmptyThreadsGracefully() {
        when(forumCategory.getThreads()).thenReturn(List.of());

        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, true);

        assertNotNull(dto.getThreads());
        assertTrue(dto.getThreads().isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        ForumCategoryDTO dto = new ForumCategoryDTO();

        dto.setId(10L);
        dto.setName("Announcements");
        dto.setDescription("Official updates");
        dto.setColor("#123456");
        dto.setThreads(List.of(mock(ForumThreadDTO.class)));

        assertEquals(10L, dto.getId());
        assertEquals("Announcements", dto.getName());
        assertEquals("Official updates", dto.getDescription());
        assertEquals("#123456", dto.getColor());
        assertEquals(1, dto.getThreads().size());
    }

    @Test
    void testConstructorHandlesNullCategoryFieldsGracefully() {
        when(forumCategory.getName()).thenReturn(null);
        when(forumCategory.getDescription()).thenReturn(null);
        when(forumCategory.getColor()).thenReturn(null);

        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, false);

        assertNull(dto.getName());
        assertNull(dto.getDescription());
        assertNull(dto.getColor());
    }
}
