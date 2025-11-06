package fr.airsen.api.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ForumMessage entity.
 */
class ForumMessageTest {

    private ForumMessage message;
    private ForumThread thread;
    private User activeUser;
    private Validator validator;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        thread = mock(ForumThread.class);
        activeUser = mock(User.class);
        now = LocalDateTime.now();

        when(activeUser.getDisplayName()).thenReturn("Alice Martin");
        when(activeUser.getId()).thenReturn(42L);
        when(activeUser.hasProfileLink()).thenReturn(true);

        message = new ForumMessage(activeUser, thread, "Hello World", now);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // --- Constructors and basic fields ---

    @Test
    void testDefaultConstructor() {
        ForumMessage empty = new ForumMessage();
        assertNull(empty.getAuthor());
        assertNull(empty.getThread());
        assertNull(empty.getContent());
        assertNull(empty.getCreatedDate());
        assertFalse(empty.getAuthorDeleted());
        assertNull(empty.getAuthorName());
    }

    @Test
    void testParameterizedConstructor() {
        assertEquals(activeUser, message.getAuthor());
        assertEquals(thread, message.getThread());
        assertEquals("Hello World", message.getContent());
        assertEquals(now, message.getCreatedDate());
        assertFalse(message.getAuthorDeleted());
        assertNull(message.getAuthorName());
    }

    @Test
    void testSettersAndGetters() {
        ForumMessage m = new ForumMessage();
        m.setId(10L);
        m.setAuthor(activeUser);
        m.setThread(thread);
        m.setContent("New content");
        m.setCreatedDate(now);
        m.setAuthorDeleted(true);
        m.setAuthorName("Old Name");

        assertEquals(10L, m.getId());
        assertEquals(activeUser, m.getAuthor());
        assertEquals(thread, m.getThread());
        assertEquals("New content", m.getContent());
        assertEquals(now, m.getCreatedDate());
        assertTrue(m.getAuthorDeleted());
        assertEquals("Old Name", m.getAuthorName());
    }

    // --- Business logic: getAuthorDisplayName() ---

    @Test
    void testGetAuthorDisplayName_activeAuthor() {
        assertEquals("Alice Martin", message.getAuthorDisplayName());
    }

    @Test
    void testGetAuthorDisplayName_deletedAuthor_preservedName() {
        ForumMessage m = new ForumMessage(null, thread, "Content", now);
        m.setAuthorDeleted(true);
        m.setAuthorName("Deleted User");
        assertEquals("Deleted User", m.getAuthorDisplayName());
    }

    @Test
    void testGetAuthorDisplayName_bothNull_returnsUnknownUser() {
        ForumMessage m = new ForumMessage(null, thread, "Message", now);
        m.setAuthorDeleted(true);
        m.setAuthorName(null);
        assertEquals("Unknown User", m.getAuthorDisplayName());
    }

    // --- Business logic: hasAuthorProfileLink() ---

    @Test
    void testHasAuthorProfileLink_activeUserWithLink() {
        assertTrue(message.hasAuthorProfileLink());
    }

    @Test
    void testHasAuthorProfileLink_activeUserWithoutLink() {
        User user = mock(User.class);
        when(user.hasProfileLink()).thenReturn(false);
        ForumMessage m = new ForumMessage(user, thread, "Msg", now);
        assertFalse(m.hasAuthorProfileLink());
    }

    @Test
    void testHasAuthorProfileLink_deletedAuthor_returnsFalse() {
        ForumMessage m = new ForumMessage(null, thread, "Msg", now);
        m.setAuthorDeleted(true);
        assertFalse(m.hasAuthorProfileLink());
    }

    // --- Business logic: getAuthorIdForLink() ---

    @Test
    void testGetAuthorIdForLink_withProfileLink() {
        assertEquals(42L, message.getAuthorIdForLink());
    }

    @Test
    void testGetAuthorIdForLink_noProfileLink_returnsNull() {
        User user = mock(User.class);
        when(user.hasProfileLink()).thenReturn(false);
        ForumMessage m = new ForumMessage(user, thread, "Msg", now);
        assertNull(m.getAuthorIdForLink());
    }

    @Test
    void testGetAuthorIdForLink_deletedAuthor_returnsNull() {
        ForumMessage m = new ForumMessage(null, thread, "Msg", now);
        assertNull(m.getAuthorIdForLink());
    }

    // --- equals(), hashCode(), toString() ---

    @Test
    void testEqualsAndHashCode_sameData_shouldBeEqual() {
        ForumMessage m1 = new ForumMessage(activeUser, thread, "Hello", now);
        ForumMessage m2 = new ForumMessage(activeUser, thread, "Hello", now);
        m1.setId(1L);
        m2.setId(1L);

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testEquals_differentData_shouldNotBeEqual() {
        ForumMessage m1 = new ForumMessage(activeUser, thread, "A", now);
        ForumMessage m2 = new ForumMessage(activeUser, thread, "B", now);
        m1.setId(1L);
        m2.setId(2L);
        assertNotEquals(m1, m2);
    }

    @Test
    void testToString_containsExpectedValues() {
        String s = message.toString();
        assertTrue(s.contains("ForumMessage"));
        assertTrue(s.contains("content='Hello World'"));
        assertTrue(s.contains("createdDate="));
    }

    // --- Validation constraints ---

    @Test
    void testContentEmpty_shouldFailValidation() {
        message.setContent("");
        Set<ConstraintViolation<ForumMessage>> violations = validator.validate(message);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be empty")));
    }

    @Test
    void testAuthorNameTooLong_shouldFailValidation() {
        String longName = "A".repeat(201);
        message.setAuthorName(longName);
        Set<ConstraintViolation<ForumMessage>> violations = validator.validate(message);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot exceed 200 characters")));
    }

    @Test
    void testValidEntity_shouldPassValidation() {
        Set<ConstraintViolation<ForumMessage>> violations = validator.validate(message);
        assertTrue(violations.isEmpty());
    }
}
