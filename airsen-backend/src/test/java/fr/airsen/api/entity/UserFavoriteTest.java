package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserFavorite entity tests")
class UserFavoriteTest {

    @Test
    @DisplayName("Constructor should create composite id and set relationships")
    void constructor_setsIdAndRelationships() {
        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setPassword("secret");
        user.setRole(UserRole.USER);

        Region region = new Region("Occitanie", "76");
        region.setId(5L);

        Department department = new Department("Herault", "34", "76", region);
        department.setId(8L);

        Commune commune = new Commune();
        commune.setInseeCode("34172");
        commune.setName("Montpellier");
        commune.setDepartment(department);
        commune.setPopulation(100000);
        commune.setLatitude(new BigDecimal("43.6108"));
        commune.setLongitude(new BigDecimal("3.8767"));

        UserFavorite favorite = new UserFavorite(user, commune);

        assertNotNull(favorite.getId());
        assertEquals(new UserFavoriteId(10L, "34172"), favorite.getId());
        assertEquals(user, favorite.getUser());
        assertEquals(commune, favorite.getCommune());
    }

    @Test
    @DisplayName("equals/hashCode should rely on composite id")
    void equalsHashCode_basedOnId() {
        UserFavorite favorite1 = new UserFavorite();
        favorite1.setId(new UserFavoriteId(1L, "12345"));

        UserFavorite favorite2 = new UserFavorite();
        favorite2.setId(new UserFavoriteId(1L, "12345"));

        UserFavorite favorite3 = new UserFavorite();
        favorite3.setId(new UserFavoriteId(2L, "67890"));

        assertEquals(favorite1, favorite2);
        assertEquals(favorite1.hashCode(), favorite2.hashCode());
        assertNotEquals(favorite1, favorite3);
    }

    @Test
    @DisplayName("@PrePersist should initialize createdAt timestamp")
    void prePersist_setsCreatedAt() {
        UserFavorite favorite = new UserFavorite();
        assertNull(favorite.getCreatedAt());

        favorite.onCreate();

        assertNotNull(favorite.getCreatedAt());
        assertTrue(favorite.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
}
