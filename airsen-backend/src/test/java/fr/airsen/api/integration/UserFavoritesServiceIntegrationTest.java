package fr.airsen.api.integration;

import fr.airsen.api.dto.request.AddFavoriteRequest;
import fr.airsen.api.dto.response.UserFavoriteResponse;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.exception.DuplicateFavoriteException;
import fr.airsen.api.exception.MaximumFavoritesExceededException;
import fr.airsen.api.exception.ResourceNotFoundException;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.DepartmentRepository;
import fr.airsen.api.repository.RegionRepository;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.service.UserFavoritesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
@DisplayName("UserFavoritesService integration tests")
class UserFavoritesServiceIntegrationTest {

    @Autowired
    private UserFavoritesService favoritesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommuneRepository communeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private User user;
    private Commune montpellier;

    @BeforeEach
    void setUp() {
        Region region = new Region("Occitanie", "76");
        region = regionRepository.save(region);

        Department department = new Department("Herault", "34", region.getRegionCode(), region);
        department = departmentRepository.save(department);

        user = new User();
        user.setEmail("integration-user@example.com");
        user.setPassword("hashed-password");
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        montpellier = createCommune("34172", "Montpellier", department);
    }

    @Test
    @DisplayName("Should add favorite and retrieve denormalized response")
    void addFavoriteAndRetrieve() {
        favoritesService.addFavorite(user.getId(), new AddFavoriteRequest(montpellier.getInseeCode()));

        List<UserFavoriteResponse> favorites = favoritesService.getUserFavorites(user.getId());

        assertEquals(1, favorites.size());
        UserFavoriteResponse favorite = favorites.get(0);
        assertEquals("34172", favorite.communeInseeCode());
        assertEquals("Montpellier", favorite.communeName());
        assertEquals("Herault", favorite.departmentName());
        assertEquals("Occitanie", favorite.regionName());
        assertNotNull(favorite.addedAt());
    }

    @Test
    @DisplayName("Should prevent duplicate favorites for same commune")
    void addFavorite_preventDuplicate() {
        favoritesService.addFavorite(user.getId(), new AddFavoriteRequest(montpellier.getInseeCode()));

        assertThrows(DuplicateFavoriteException.class, () ->
                favoritesService.addFavorite(user.getId(), new AddFavoriteRequest(montpellier.getInseeCode())));
    }

    @Test
    @DisplayName("Should enforce maximum favorites limit per user")
    void addFavorite_enforceMaximumLimit() {
        List<Commune> communes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            communes.add(createCommune("99" + String.format("%03d", i), "Commune-" + i, montpellier.getDepartment()));
        }

        communes.forEach(commune ->
                favoritesService.addFavorite(user.getId(), new AddFavoriteRequest(commune.getInseeCode())));

        Commune extraCommune = createCommune("98111", "Overflow", montpellier.getDepartment());

        assertThrows(MaximumFavoritesExceededException.class, () ->
                favoritesService.addFavorite(user.getId(), new AddFavoriteRequest(extraCommune.getInseeCode())));

        assertEquals(10, favoritesService.getFavoriteCount(user.getId()));
    }

    @Test
    @DisplayName("Should remove favorite and update count")
    void removeFavorite_success() {
        favoritesService.addFavorite(user.getId(), new AddFavoriteRequest(montpellier.getInseeCode()));
        assertTrue(favoritesService.isFavorited(user.getId(), montpellier.getInseeCode()));

        favoritesService.removeFavorite(user.getId(), montpellier.getInseeCode());

        assertEquals(0, favoritesService.getFavoriteCount(user.getId()));
        assertFalse(favoritesService.isFavorited(user.getId(), montpellier.getInseeCode()));
    }

    @Test
    @DisplayName("Removing nonexistent favorite should raise error")
    void removeFavorite_missing() {
        assertThrows(ResourceNotFoundException.class, () ->
                favoritesService.removeFavorite(user.getId(), "99999"));
    }

    @Test
    @DisplayName("Fetching favorites does not trigger N+1 queries")
    void getUserFavorites_noNPlusOne() {
        List<Commune> communes = List.of(
                createCommune("50001", "Commune-A", montpellier.getDepartment()),
                createCommune("50002", "Commune-B", montpellier.getDepartment()),
                createCommune("50003", "Commune-C", montpellier.getDepartment())
        );

        communes.forEach(commune ->
                favoritesService.addFavorite(user.getId(), new AddFavoriteRequest(commune.getInseeCode())));

        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        List<UserFavoriteResponse> favorites = favoritesService.getUserFavorites(user.getId());

        assertEquals(3, favorites.size());
        assertTrue(statistics.getPrepareStatementCount() <= 2,
                "Expected at most two SQL statements (exists check + join fetch query)");
    }

    private Commune createCommune(String inseeCode, String name, Department department) {
        Commune commune = new Commune();
        commune.setInseeCode(inseeCode);
        commune.setName(name);
        commune.setDepartment(department);
        commune.setDepartmentCode(department.getDepartmentCode());
        commune.setRegionCode(department.getRegion().getRegionCode());
        commune.setPopulation(10_000);
        commune.setLatitude(new BigDecimal("43.6000"));
        commune.setLongitude(new BigDecimal("3.8800"));
        return communeRepository.save(commune);
    }
}
