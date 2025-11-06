package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.VoteType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ForumThread entity.
 */
class ForumThreadTest {

    private ForumThread thread;
    private User activeUser;
    private ForumCategory category;
    private Validator validator;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        activeUser = mock(User.class);
        category = mock(ForumCategory.class);
        now = LocalDateTime.now();

        when(activeUser.getDisplayName()).thenReturn("Alice Martin");
        when(activeUser.getId()).thenReturn(1L);
        when(activeUser.hasProfileLink()).thenReturn(true);

        thread = new ForumThread(activeUser, category, "Thread Title", "Thread Content", now, now, 10, true, false, 5);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // --- Constructors and basic getters/setters ---

    @Test
    void testDefaultConstructor() {
        ForumThread empty = new ForumThread();
        assertNull(empty.getAuthor());
        assertNull(empty.getCategory());
        assertNull(empty.getTitle());
        assertNull(empty.getContent());
        assertNull(empty.getCreatedDate());
        assertFalse(empty.isPinned());
        assertFalse(empty.isClosed());
        assertFalse(empty.getAuthorDeleted());
        assertNull(empty.getAuthorName());
    }

    @Test
    void testParameterizedConstructor() {
        assertEquals(activeUser, thread.getAuthor());
        assertEquals(category, thread.getCategory());
        assertEquals("Thread Title", thread.getTitle());
        assertEquals("Thread Content", thread.getContent());
        assertEquals(now, thread.getCreatedDate());
        assertEquals(now, thread.getLastMessageDate());
        assertEquals(10, thread.getViewCount());
        assertTrue(thread.isPinned());
        assertFalse(thread.isClosed());
        assertEquals(5, thread.getLikeCount());
        assertFalse(thread.getAuthorDeleted());
        assertNull(thread.getAuthorName());
    }

    @Test
    void testSettersAndGetters() {
        ForumThread t = new ForumThread();
        t.setId(1L);
        t.setAuthor(activeUser);
        t.setCategory(category);
        t.setTitle("New Title");
        t.setContent("New Content");
        t.setCreatedDate(now);
        t.setLastMessageDate(now);
        t.setViewCount(20);
        t.setPinned(false);
        t.setClosed(true);
        t.setLikeCount(7);
        t.setAuthorDeleted(true);
        t.setAuthorName("Deleted Author");

        assertEquals(1L, t.getId());
        assertEquals(activeUser, t.getAuthor());
        assertEquals(category, t.getCategory());
        assertEquals("New Title", t.getTitle());
        assertEquals("New Content", t.getContent());
        assertEquals(now, t.getCreatedDate());
        assertEquals(now, t.getLastMessageDate());
        assertEquals(20, t.getViewCount());
        assertFalse(t.isPinned());
        assertTrue(t.isClosed());
        assertEquals(7, t.getLikeCount());
        assertTrue(t.getAuthorDeleted());
        assertEquals("Deleted Author", t.getAuthorName());
    }

    // --- GDPR Author Preservation methods ---

    @Test
    void testGetAuthorDisplayName_activeAuthor() {
        assertEquals("Alice Martin", thread.getAuthorDisplayName());
    }

    @Test
    void testGetAuthorDisplayName_deletedAuthor() {
        ForumThread t = new ForumThread();
        t.setAuthor(null);
        t.setAuthorDeleted(true);
        t.setAuthorName("Deleted User");
        assertEquals("Deleted User", t.getAuthorDisplayName());
    }

    @Test
    void testGetAuthorDisplayName_bothNull() {
        ForumThread t = new ForumThread();
        t.setAuthor(null);
        t.setAuthorDeleted(true);
        t.setAuthorName(null);
        assertEquals("Unknown User", t.getAuthorDisplayName());
    }

    @Test
    void testHasAuthorProfileLink_activeUserWithLink() {
        assertTrue(thread.hasAuthorProfileLink());
    }

    @Test
    void testHasAuthorProfileLink_activeUserWithoutLink() {
        User user = mock(User.class);
        when(user.hasProfileLink()).thenReturn(false);
        ForumThread t = new ForumThread();
        t.setAuthor(user);
        assertFalse(t.hasAuthorProfileLink());
    }

    @Test
    void testHasAuthorProfileLink_deletedAuthor() {
        ForumThread t = new ForumThread();
        t.setAuthor(null);
        assertFalse(t.hasAuthorProfileLink());
    }

    @Test
    void testGetAuthorIdForLink_withProfileLink() {
        assertEquals(1L, thread.getAuthorIdForLink());
    }

    @Test
    void testGetAuthorIdForLink_noProfileLink_returnsNull() {
        User user = mock(User.class);
        when(user.hasProfileLink()).thenReturn(false);
        when(user.getId()).thenReturn(2L);
        ForumThread t = new ForumThread();
        t.setAuthor(user);
        assertNull(t.getAuthorIdForLink());
    }

    @Test
    void testGetAuthorIdForLink_deletedAuthor_returnsNull() {
        ForumThread t = new ForumThread();
        t.setAuthor(null);
        assertNull(t.getAuthorIdForLink());
    }

    // --- Votes calculation ---

    @Test
    void testGetVotesValue_noVotes() {
        thread.setVotes(null);
        assertEquals(0, thread.getVotesValue());
    }

    @Test
    void testGetVotesValue_withVotes() {
        ForumVote vote1 = mock(ForumVote.class);
        ForumVote vote2 = mock(ForumVote.class);
        when(vote1.getVoteType()).thenReturn(VoteType.LIKE);
        when(vote2.getVoteType()).thenReturn(VoteType.DISLIKE);
        when(vote1.getVoteType().toInt()).thenReturn(1);
        when(vote2.getVoteType().toInt()).thenReturn(-1);

        thread.setVotes(List.of(vote1, vote2));
        assertEquals(0, thread.getVotesValue());
    }

    // --- equals(), hashCode(), toString() ---

    @Test
    void testEqualsAndHashCode_sameData() {
        ForumThread t1 = new ForumThread(activeUser, category, "Thread Title", "Content", now, now, 0, false, false, 0);
        ForumThread t2 = new ForumThread(activeUser, category, "Thread Title", "Other Content", now, now, 5, true, true, 10);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testEquals_differentData() {
        ForumThread t1 = new ForumThread(activeUser, category, "A", "Content", now, now, 0, false, false, 0);
        ForumThread t2 = new ForumThread(activeUser, category, "B", "Content", now, now, 0, false, false, 0);
        assertNotEquals(t1, t2);
    }

    @Test
    void testToString_containsExpectedValues() {
        String s = thread.toString();
        assertTrue(s.contains("ForumThread"));
        assertTrue(s.contains("Thread Title"));
        assertTrue(s.contains("Thread Content"));
    }

    // --- Validation constraints ---

    @Test
    void testTitleNotBlank_shouldFailValidation() {
        thread.setTitle("");
        Set<ConstraintViolation<ForumThread>> violations = validator.validate(thread);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("required")));
    }

    @Test
    void testContentNotBlank_shouldFailValidation() {
        thread.setContent("");
        Set<ConstraintViolation<ForumThread>> violations = validator.validate(thread);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("required")));
    }

    @Test
    void testAuthorNameTooLong_shouldFailValidation() {
        thread.setAuthorName("A".repeat(201));
        Set<ConstraintViolation<ForumThread>> violations = validator.validate(thread);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot exceed 200 characters")));
    }

    @Test
    void testValidEntity_shouldPassValidation() {
        Set<ConstraintViolation<ForumThread>> violations = validator.validate(thread);
        assertTrue(violations.isEmpty());
    }
}
