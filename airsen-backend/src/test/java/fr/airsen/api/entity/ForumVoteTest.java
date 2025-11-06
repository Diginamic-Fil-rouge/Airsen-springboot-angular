package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ForumVote entity.
 */
class ForumVoteTest {

    private ForumVote vote;
    private User user;
    private ForumThread thread;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        thread = mock(ForumThread.class);

        vote = new ForumVote(user, thread, VoteType.LIKE);
    }

    // --- Constructors and getters/setters ---

    @Test
    void testDefaultConstructor() {
        ForumVote v = new ForumVote();
        assertNull(v.getUser());
        assertNull(v.getThread());
        assertNull(v.getVoteType());
        assertFalse(v.getUserDeleted());
    }

    @Test
    void testParameterizedConstructor() {
        assertEquals(user, vote.getUser());
        assertEquals(thread, vote.getThread());
        assertEquals(VoteType.LIKE, vote.getVoteType());
        assertFalse(vote.getUserDeleted());
    }

    @Test
    void testSettersAndGetters() {
        ForumVote v = new ForumVote();
        v.setId(1L);
        v.setUser(user);
        v.setThread(thread);
        v.setVoteType(VoteType.DISLIKE);
        v.setUserDeleted(true);

        assertEquals(1L, v.getId());
        assertEquals(user, v.getUser());
        assertEquals(thread, v.getThread());
        assertEquals(VoteType.DISLIKE, v.getVoteType());
        assertTrue(v.getUserDeleted());
    }

    // --- equals() and hashCode() ---

    @Test
    void testEqualsAndHashCode_sameData() {
        ForumVote v1 = new ForumVote(user, thread, VoteType.LIKE);
        ForumVote v2 = new ForumVote(user, thread, VoteType.DISLIKE);
        v1.setId(10L);
        v2.setId(10L);

        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void testEquals_differentData() {
        ForumVote v1 = new ForumVote(user, thread, VoteType.LIKE);
        ForumVote v2 = new ForumVote(user, thread, VoteType.LIKE);
        v1.setId(1L);
        v2.setId(2L);
        assertNotEquals(v1, v2);
    }

    @Test
    void testEquals_withNonForumVote_shouldReturnFalse() {
        assertNotEquals(vote, "not a vote");
    }

    // --- toString() ---

    @Test
    void testToString_containsExpectedValues() {
        String s = vote.toString();
        assertTrue(s.contains("ForumVote"));
        assertTrue(s.contains("voteType=LIKE"));
    }

    // --- GDPR voter anonymization logic ---

    @Test
    void testUserDeletedFlag_defaultsFalse() {
        ForumVote v = new ForumVote();
        assertFalse(v.getUserDeleted());
    }

    @Test
    void testUserDeletedFlag_setTrue() {
        vote.setUserDeleted(true);
        assertTrue(vote.getUserDeleted());
    }
}
