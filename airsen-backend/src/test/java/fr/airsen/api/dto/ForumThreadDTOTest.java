package fr.airsen.api.dto;

import fr.airsen.api.entity.*;
import fr.airsen.api.entity.enums.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumThreadDTOTest {

    private ForumThread forumThread;
    private User author;
    private ForumCategory category;
    private ForumMessage message;
    private ForumVote vote;

    @BeforeEach
    void setUp() {
        forumThread = mock(ForumThread.class);
        author = mock(User.class);
        category = mock(ForumCategory.class);
        message = mock(ForumMessage.class);
        vote = mock(ForumVote.class);

        when(message.getAuthor()).thenReturn(author);
        when(vote.getUser()).thenReturn(author);
        when(vote.getThread()).thenReturn(forumThread);

        when(forumThread.getId()).thenReturn(100L);
        when(forumThread.getAuthor()).thenReturn(author);
        when(forumThread.getCategory()).thenReturn(category);
        when(forumThread.getTitle()).thenReturn("Test Thread");
        when(forumThread.getContent()).thenReturn("Thread content");
        when(forumThread.getCreatedDate()).thenReturn(LocalDateTime.of(2025, 1, 4, 10, 0));
        when(forumThread.getLastMessageDate()).thenReturn(LocalDateTime.of(2025, 1, 5, 14, 30));
        when(forumThread.getViewCount()).thenReturn(42);
        when(forumThread.isPinned()).thenReturn(true);
        when(forumThread.isClosed()).thenReturn(false);
        when(forumThread.getVotesValue()).thenReturn(15);

        when(forumThread.getMessages()).thenReturn(List.of(message));
        when(forumThread.getVotes()).thenReturn(List.of(vote));
    }

    @Test
    void testDefaultConstructor() {
        ForumThreadDTO dto = new ForumThreadDTO();

        assertNotNull(dto);
        assertEquals(0L, dto.getId());
        assertNull(dto.getAuthor());
        assertNull(dto.getCategory());
        assertNull(dto.getMessages());
        assertNull(dto.getVotes());
        assertNull(dto.getTitle());
        assertNull(dto.getContent());
    }

    @Test
    void testConstructorWithEntitiesTrue() {
        ForumThreadDTO dto = new ForumThreadDTO(forumThread, true);

        assertEquals(100L, dto.getId());
        assertNotNull(dto.getAuthor());
        assertNotNull(dto.getCategory());
        assertEquals("Test Thread", dto.getTitle());
        assertEquals("Thread content", dto.getContent());
        assertEquals(LocalDateTime.of(2025, 1, 4, 10, 0), dto.getCreatedDate());
        assertEquals(LocalDateTime.of(2025, 1, 5, 14, 30), dto.getLastMessageDate());
        assertEquals(42, dto.getViewCount());
        assertTrue(dto.isPinned());
        assertFalse(dto.isClosed());
        assertEquals(15, dto.getLikeCount());
        assertEquals(1, dto.getMessageCount());

        // WithEntities should populate nested DTOs
        assertNotNull(dto.getMessages());
        assertNotNull(dto.getVotes());
        assertEquals(1, dto.getMessages().size());
        assertEquals(1, dto.getVotes().size());
    }

    @Test
    void testConstructorWithEntitiesFalse() {
        ForumThreadDTO dto = new ForumThreadDTO(forumThread, false);

        assertEquals(100L, dto.getId());
        assertNotNull(dto.getAuthor());
        assertNotNull(dto.getCategory());
        assertEquals("Test Thread", dto.getTitle());
        assertEquals("Thread content", dto.getContent());
        assertEquals(42, dto.getViewCount());
        assertTrue(dto.isPinned());
        assertFalse(dto.isClosed());
        assertEquals(15, dto.getLikeCount());
        assertEquals(1, dto.getMessageCount());

        // WithEntities=false should not populate nested lists
        assertNull(dto.getMessages());
        assertNull(dto.getVotes());
    }

    @Test
    void testConstructorHandlesNullMessagesAndVotes() {
        when(forumThread.getMessages()).thenReturn(null);
        when(forumThread.getVotes()).thenReturn(null);

        ForumThreadDTO dto = new ForumThreadDTO(forumThread, true);

        assertEquals(0, dto.getMessageCount());
        assertNotNull(dto.getMessages());
        assertTrue(dto.getMessages().isEmpty());
        assertNotNull(dto.getVotes());
        assertTrue(dto.getVotes().isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        ForumThreadDTO dto = new ForumThreadDTO();

        ForumAuthorDTO authorDTO = mock(ForumAuthorDTO.class);
        ForumCategoryDTO categoryDTO = mock(ForumCategoryDTO.class);
        ForumMessageDTO messageDTO = mock(ForumMessageDTO.class);
        ForumVoteDTO voteDTO = mock(ForumVoteDTO.class);
        LocalDateTime now = LocalDateTime.now();

        dto.setId(200L);
        dto.setAuthor(authorDTO);
        dto.setCategory(categoryDTO);
        dto.setMessages(List.of(messageDTO));
        dto.setVotes(List.of(voteDTO));
        dto.setTitle("Updated Title");
        dto.setContent("Updated Content");
        dto.setCreatedDate(now);
        dto.setLastMessageDate(now.plusDays(1));
        dto.setViewCount(99);
        dto.setPinned(false);
        dto.setClosed(true);
        dto.setLikeCount(5);
        dto.setMessageCount(2);

        assertEquals(200L, dto.getId());
        assertEquals(authorDTO, dto.getAuthor());
        assertEquals(categoryDTO, dto.getCategory());
        assertEquals(1, dto.getMessages().size());
        assertEquals(1, dto.getVotes().size());
        assertEquals("Updated Title", dto.getTitle());
        assertEquals("Updated Content", dto.getContent());
        assertEquals(now, dto.getCreatedDate());
        assertEquals(now.plusDays(1), dto.getLastMessageDate());
        assertEquals(99, dto.getViewCount());
        assertFalse(dto.isPinned());
        assertTrue(dto.isClosed());
        assertEquals(5, dto.getLikeCount());
        assertEquals(2, dto.getMessageCount());
    }
}