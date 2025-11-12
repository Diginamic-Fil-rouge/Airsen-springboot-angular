package fr.airsen.api.dto;

import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumCategoryDTOTest {

    private ForumCategory forumCategory;
    private ForumThread forumThread1;
    private ForumThread forumThread2;

    @BeforeEach
    void setUp() {
        forumCategory = mock(ForumCategory.class);
        forumThread1 = mock(ForumThread.class);
        forumThread2 = mock(ForumThread.class);

        when(forumCategory.getId()).thenReturn(1L);
        when(forumCategory.getName()).thenReturn("General Discussion");
        when(forumCategory.getDescription()).thenReturn("A place to chat about anything.");
        when(forumCategory.getColor()).thenReturn("#FF0000");
        when(forumThread1.getCategory()).thenReturn(forumCategory);
        when(forumThread2.getCategory()).thenReturn(forumCategory);
    }

    @Test
    void testEmptyConstructor() {
        ForumCategoryDTO dto = new ForumCategoryDTO();
        assertNotNull(dto);
        assertNull(dto.getName());
        assertNull(dto.getDescription());
        assertNull(dto.getColor());
        assertNull(dto.getThreads());
    }

    @Test
    void testConstructorWithoutThreads() {
        when(forumCategory.getThreads()).thenReturn(Arrays.asList(forumThread1, forumThread2));

        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, false);

        assertEquals(1L, dto.getId());
        assertEquals("General Discussion", dto.getName());
        assertEquals("A place to chat about anything.", dto.getDescription());
        assertEquals("#FF0000", dto.getColor());
        assertNotNull(dto.getThreads());
        assertTrue(dto.getThreads().isEmpty(), "Threads should not be loaded when withThreads = false");
    }

    @Test
    void testConstructorWithThreads() {
        when(forumCategory.getThreads()).thenReturn(Arrays.asList(forumThread1, forumThread2));

        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, true);

        assertEquals(1L, dto.getId());
        assertEquals("General Discussion", dto.getName());
        assertEquals("A place to chat about anything.", dto.getDescription());
        assertEquals("#FF0000", dto.getColor());
        assertNotNull(dto.getThreads());
        assertEquals(2, dto.getThreads().size());
    }

    @Test
    void testConstructorWithNullThreads() {
        when(forumCategory.getThreads()).thenReturn(null);

        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, true);

        assertNotNull(dto.getThreads());
        assertTrue(dto.getThreads().isEmpty(), "Threads should be empty when source list is null");
    }

    @Test
    void testConstructorWithEmptyThreads() {
        when(forumCategory.getThreads()).thenReturn(new ArrayList<>());

        ForumCategoryDTO dto = new ForumCategoryDTO(forumCategory, true);

        assertNotNull(dto.getThreads());
        assertTrue(dto.getThreads().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        ForumCategoryDTO dto = new ForumCategoryDTO();
        dto.setId(42L);
        dto.setName("Tech");
        dto.setDescription("Technology discussions");
        dto.setColor("#00FF00");

        List<ForumThreadDTO> threadList = new ArrayList<>();
        dto.setThreads(threadList);

        assertEquals(42L, dto.getId());
        assertEquals("Tech", dto.getName());
        assertEquals("Technology discussions", dto.getDescription());
        assertEquals("#00FF00", dto.getColor());
        assertSame(threadList, dto.getThreads());
    }

    @Test
    void testConstructorDoesNotFailWithEmptyCategory() {
        ForumCategory emptyCategory = mock(ForumCategory.class);
        when(emptyCategory.getThreads()).thenReturn(null);

        assertDoesNotThrow(() -> new ForumCategoryDTO(emptyCategory, true));
    }
}
