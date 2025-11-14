package fr.airsen.api.repository;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.*;
import fr.airsen.api.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserFavoriteRepository integration tests")
@Transactional
class UserFavoriteRepositoryTest extends AbstractTestContainersTest {

    @Autowired
    private UserFavoriteRepository userFavoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommuneRepository communeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegionRepository regionRepository;

    private User persistedUser;
    private Commune montpellier;
    private Commune paris;

    @BeforeEach
    void setUp() {
        persistedUser = persistUser("favorite-user@example.com");

        Region regionOccitanie = persistRegion("76", "Occitanie");
        Department herault = persistDepartment("34", "Herault", regionOccitanie);
        montpellier = persistCommune("34172", "Montpellier", herault);

        Region regionIleDeFrance = persistRegion("11", "Ile-de-France");
        Department parisDepartment = persistDepartment("75", "Paris", regionIleDeFrance);
        paris = persistCommune("75056", "Paris", parisDepartment);
    }

    @Test
    @DisplayName("findByUserId should return favorites with loaded commune hierarchy")
    void findByUserId_returnsFavoritesWithCommuneHierarchy() {
        persistFavorite(persistedUser, montpellier);
        persistFavorite(persistedUser, paris);

        List<UserFavorite> favorites = userFavoriteRepository.findByUserId(persistedUser.getId());

        assertEquals(2, favorites.size());
        assertEquals("Paris", favorites.get(0).getCommune().getName());
        assertEquals("Montpellier", favorites.get(1).getCommune().getName());
        assertEquals("Paris", favorites.get(0).getCommune().getDepartment().getName());
        assertEquals("Ile-de-France", favorites.get(0).getCommune().getDepartment().getRegion().getName());
    }

    @Test
    @DisplayName("countByUserId should return number of favorites")
    void countByUserId_returnsCount() {
        persistFavorite(persistedUser, montpellier);
        persistFavorite(persistedUser, paris);

        int count = userFavoriteRepository.countByUserId(persistedUser.getId());

        assertEquals(2, count);
    }

    @Test
    @DisplayName("existsById_UserIdAndId_CommuneInseeCode should detect existing favorite")
    void existsByCompositeKey_returnsTrueWhenFavoriteExists() {
        persistFavorite(persistedUser, montpellier);

        boolean exists = userFavoriteRepository.existsById_UserIdAndId_CommuneInseeCode(persistedUser.getId(), montpellier.getInseeCode());

        assertTrue(exists);
    }

    @Test
    @DisplayName("deleteByUserIdAndCommuneInseeCode should remove favorite")
    void deleteByUserIdAndCommuneInseeCode_removesFavorite() {
        persistFavorite(persistedUser, montpellier);

        userFavoriteRepository.deleteByUserIdAndCommuneInseeCode(persistedUser.getId(), montpellier.getInseeCode());

        boolean exists = userFavoriteRepository.existsById_UserIdAndId_CommuneInseeCode(persistedUser.getId(), montpellier.getInseeCode());
        assertFalse(exists);
    }

    @Test
    @DisplayName("deleteAllByUserId should remove all favorites for user")
    void deleteAllByUserId_removesAllFavorites() {
        persistFavorite(persistedUser, montpellier);
        persistFavorite(persistedUser, paris);

        userFavoriteRepository.deleteAllByUserId(persistedUser.getId());

        int count = userFavoriteRepository.countByUserId(persistedUser.getId());
        assertEquals(0, count);
    }

    @Test
    @DisplayName("findMostFavoritedCommunes should return communes ordered by popularity")
    void findMostFavoritedCommunes_returnsPopularityRanking() {
        User otherUser = persistUser("another-user@example.com");
        persistFavorite(persistedUser, montpellier);
        persistFavorite(persistedUser, paris);
        persistFavorite(otherUser, paris);

        List<Object[]> results = userFavoriteRepository.findMostFavoritedCommunes(PageRequest.of(0, 10));

        assertEquals(2, results.size());
        Commune mostPopular = (Commune) results.get(0)[0];
        Long parisCount = (Long) results.get(0)[1];
        assertEquals("Paris", mostPopular.getName());
        assertEquals(2L, parisCount);
    }

    @Test
    @DisplayName("findUserIdsByCommuneInseeCode should return all user ids favoriting the commune")
    void findUserIdsByCommuneInseeCode_returnsUserIds() {
        User otherUser = persistUser("third-user@example.com");
        persistFavorite(persistedUser, montpellier);
        persistFavorite(otherUser, montpellier);

        List<Long> userIds = userFavoriteRepository.findUserIdsByCommuneInseeCode(montpellier.getInseeCode());

        assertEquals(2, userIds.size());
        assertTrue(userIds.contains(persistedUser.getId()));
        assertTrue(userIds.contains(otherUser.getId()));
    }

    private UserFavorite persistFavorite(User user, Commune commune) {
        UserFavorite favorite = new UserFavorite(user, commune);
        return userFavoriteRepository.save(favorite);
    }

    private Commune persistCommune(String inseeCode, String name, Department department) {
        Commune commune = new Commune();
        commune.setInseeCode(inseeCode);
        commune.setName(name);
        commune.setDepartment(department);
        commune.setDepartmentCode(department.getDepartmentCode());
        commune.setRegionCode(department.getRegion().getRegionCode());
        commune.setPopulation(100_000);
        commune.setLatitude(new BigDecimal("43.6000"));
        commune.setLongitude(new BigDecimal("3.8800"));
        return communeRepository.save(commune);
    }

    private Department persistDepartment(String code, String name, Region region) {
        Department department = new Department(name, code, region.getRegionCode(), region);
        return departmentRepository.save(department);
    }

    private Region persistRegion(String code, String name) {
        Region region = new Region(name, code);
        return regionRepository.save(region);
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("secure-password");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}
