package fr.airsen.api.dto;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.entity.enums.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumVoteDTOTest {

    private ForumCategory category;
    private ForumVote forumVote;
    private User user;
    private ForumThread thread;

    @BeforeEach
    void setUp() {
        category = mock(ForumCategory.class);
        user = mock(User.class);
        thread = mock(ForumThread.class);
        forumVote = mock(ForumVote.class);

        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("john.doe@example.com");
        when(user.getFirstName()).thenReturn("John");
        when(user.getLastName()).thenReturn("Doe");
        when(user.getAddress()).thenReturn("123 Main St");
        when(user.getTelephone()).thenReturn("123456789");
        when(user.getBio()).thenReturn("Developer");
        when(user.getRole()).thenReturn(UserRole.USER);

        when(category.getId()).thenReturn(1L);
        when(category.getName()).thenReturn("Category");

        when(thread.getCategory()).thenReturn( category );

        when(forumVote.getId()).thenReturn(10L);
        when(forumVote.getUser()).thenReturn(user);
        when(forumVote.getThread()).thenReturn(thread);
        when(forumVote.getVoteType()).thenReturn(VoteType.LIKE);
    }

    @Test
    void testDefaultConstructor() {
        ForumVoteDTO dto = new ForumVoteDTO();
        assertNotNull(dto);
        assertEquals(0L, dto.getId());
        assertNull(dto.getUser());
        assertNull(dto.getThread());
        assertNull(dto.getVoteType());
    }

    @Test
    void testConstructorWithEntitiesTrue() {
        ForumVoteDTO dto = new ForumVoteDTO(forumVote, true);

        assertEquals(10L, dto.getId());
        assertEquals(VoteType.LIKE, dto.getVoteType());
        assertNotNull(dto.getUser());
        assertEquals("john.doe@example.com", dto.getUser().getEmail());
        assertNotNull(dto.getThread());
    }

    @Test
    void testConstructorWithEntitiesFalse() {
        ForumVoteDTO dto = new ForumVoteDTO(forumVote, false);

        assertEquals(10L, dto.getId());
        assertEquals(VoteType.LIKE, dto.getVoteType());
        assertNull(dto.getUser());
        assertNull(dto.getThread());
    }

    @Test
    void testGettersAndSetters() {
        ForumVoteDTO dto = new ForumVoteDTO();

        UserDTO userDTO = new UserDTO(1L, "jane@example.com", "Jane", "Doe", "456 Elm St", "987654321", "Bio", UserRole.ADMIN);
        ForumThreadDTO threadDTO = mock(ForumThreadDTO.class);

        dto.setId(5L);
        dto.setUser(userDTO);
        dto.setThread(threadDTO);
        dto.setVoteType(VoteType.DISLIKE);

        assertEquals(5L, dto.getId());
        assertEquals(userDTO, dto.getUser());
        assertEquals(threadDTO, dto.getThread());
        assertEquals(VoteType.DISLIKE, dto.getVoteType());
    }
}
