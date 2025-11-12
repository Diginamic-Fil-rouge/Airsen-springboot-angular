package fr.airsen.api.dto;

import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumAuthorDTOTest {

    @Test
    void testDefaultConstructor() {
        ForumAuthorDTO dto = new ForumAuthorDTO();

        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getRole());
    }

    @Test
    void testConstructorFromUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getFirstName()).thenReturn("Alice");
        when(user.getLastName()).thenReturn("Smith");
        when(user.getRole()).thenReturn(UserRole.USER);

        ForumAuthorDTO dto = new ForumAuthorDTO(user);

        assertEquals(1L, dto.getId());
        assertEquals("Alice", dto.getFirstName());
        assertEquals("Smith", dto.getLastName());
        assertEquals(UserRole.USER, dto.getRole());
    }

    @Test
    void testConstructorFromUserWithNullUser() {
        ForumAuthorDTO dto = new ForumAuthorDTO((User) null);

        assertNull(dto.getId());
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getRole());
    }

    @Test
    void testFullConstructor() {
        ForumAuthorDTO dto = new ForumAuthorDTO(5L, "Bob", "Brown", UserRole.ADMIN);

        assertEquals(5L, dto.getId());
        assertEquals("Bob", dto.getFirstName());
        assertEquals("Brown", dto.getLastName());
        assertEquals(UserRole.ADMIN, dto.getRole());
    }

    @Test
    void testGettersAndSetters() {
        ForumAuthorDTO dto = new ForumAuthorDTO();

        dto.setId(10L);
        dto.setFirstName("Charlie");
        dto.setLastName("Green");
        dto.setRole(UserRole.USER);

        assertEquals(10L, dto.getId());
        assertEquals("Charlie", dto.getFirstName());
        assertEquals("Green", dto.getLastName());
        assertEquals(UserRole.USER, dto.getRole());
    }

    @Test
    void testGetFullNameBothNamesPresent() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, "Jane", "Doe", UserRole.USER);
        assertEquals("Jane Doe", dto.getFullName());
    }

    @Test
    void testGetFullNameOnlyFirstName() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, "Jane", null, UserRole.USER);
        assertEquals("Jane", dto.getFullName());
    }

    @Test
    void testGetFullNameOnlyLastName() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, null, "Doe", UserRole.USER);
        assertEquals("Doe", dto.getFullName());
    }

    @Test
    void testGetFullNameNoNames() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, null, null, UserRole.USER);
        assertEquals("Unknown", dto.getFullName());
    }

    @Test
    void testGetInitialsBothNames() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, "John", "Doe", UserRole.USER);
        assertEquals("JD", dto.getInitials());
    }

    @Test
    void testGetInitialsOnlyFirstName() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, "John", null, UserRole.USER);
        assertEquals("J", dto.getInitials());
    }

    @Test
    void testGetInitialsOnlyLastName() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, null, "Doe", UserRole.USER);
        assertEquals("D", dto.getInitials());
    }

    @Test
    void testGetInitialsNoNames() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, null, null, UserRole.USER);
        assertEquals("?", dto.getInitials());
    }

    @Test
    void testGetInitialsHandlesEmptyStrings() {
        ForumAuthorDTO dto = new ForumAuthorDTO(null, "", "", UserRole.USER);
        assertEquals("?", dto.getInitials());
    }

    @Test
    void testToStringContainsAllFields() {
        ForumAuthorDTO dto = new ForumAuthorDTO(3L, "Eve", "Stone", UserRole.ADMIN);
        String str = dto.toString();

        assertTrue(str.contains("id=3"));
        assertTrue(str.contains("Eve"));
        assertTrue(str.contains("Stone"));
        assertTrue(str.contains("Administrator"));
    }
}
