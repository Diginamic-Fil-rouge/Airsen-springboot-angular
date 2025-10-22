package fr.airsen.api.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserFavoriteId entity tests")
class UserFavoriteIdTest {

    @Test
    @DisplayName("equals/hashCode - same values should be equal")
    void equalsHashCode_sameValues() {
        UserFavoriteId id1 = new UserFavoriteId(1L, "12345");
        UserFavoriteId id2 = new UserFavoriteId(1L, "12345");

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("equals - different values should not be equal")
    void equals_differentValues() {
        UserFavoriteId id1 = new UserFavoriteId(1L, "12345");
        UserFavoriteId id2 = new UserFavoriteId(2L, "67890");

        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("validate - throws when components missing")
    void validate_missingComponents() {
        UserFavoriteId id = new UserFavoriteId(null, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, id::validate);
        assertEquals("UserFavoriteId requires both userId and communeId", exception.getMessage());
    }

    @Test
    @DisplayName("validate - passes when both components set")
    void validate_validComponents() {
        UserFavoriteId id = new UserFavoriteId(1L, "12345");

        assertDoesNotThrow(id::validate);
    }
}
