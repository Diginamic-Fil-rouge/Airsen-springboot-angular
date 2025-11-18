package fr.airsen.api.service;

import fr.airsen.api.dto.request.AddFavoriteRequest;
import fr.airsen.api.dto.response.UserFavoriteResponse;
import fr.airsen.api.entity.*;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.exception.DuplicateFavoriteException;
import fr.airsen.api.exception.MaximumFavoritesExceededException;
import fr.airsen.api.exception.ResourceNotFoundException;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.UserFavoriteRepository;
import fr.airsen.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserFavoritesService tests")
class UserFavoritesServiceTest {

    @Mock
    private UserFavoriteRepository favoriteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommuneRepository communeRepository;

    @InjectMocks
    private UserFavoritesService userFavoritesService;

    private User user;
    private Commune commune;
    private Department department;
    private Region region;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPassword("secret");
        user.setRole(UserRole.USER);

        region = new Region("Occitanie", "76");
        department = new Department("Herault", "34", "76", region);

        commune = new Commune();
        commune.setInseeCode("34172");
        commune.setName("Montpellier");
        commune.setDepartment(department);
        commune.setDepartmentCode(department.getDepartmentCode());
        commune.setRegionCode(region.getRegionCode());
        commune.setPopulation(100_000);
        commune.setLatitude(new BigDecimal("43.6"));
        commune.setLongitude(new BigDecimal("3.88"));
    }

    @Test
    @DisplayName("getUserFavorites returns mapped responses when user exists")
    void getUserFavorites_success() {
        UserFavorite favorite = new UserFavorite();
        favorite.setId(new UserFavoriteId(user.getId(), commune.getInseeCode()));
        favorite.setUser(user);
        favorite.setCommune(commune);
        favorite.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));

        when(userRepository.existsById(user.getId())).thenReturn(true);
        when(favoriteRepository.findByUserId(user.getId())).thenReturn(List.of(favorite));

        List<UserFavoriteResponse> responses = userFavoritesService.getUserFavorites(user.getId());

        assertEquals(1, responses.size());
        UserFavoriteResponse response = responses.get(0);
        assertEquals("34172", response.communeInseeCode());
        assertEquals("Montpellier", response.communeName());
        assertEquals("Herault", response.departmentName());
        assertEquals("Occitanie", response.regionName());
        assertEquals(LocalDateTime.of(2024, 1, 1, 12, 0), response.addedAt());
    }

    @Test
    @DisplayName("getUserFavorites throws when user missing")
    void getUserFavorites_userMissing() {
        when(userRepository.existsById(user.getId())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userFavoritesService.getUserFavorites(user.getId()));
    }

    @Test
    @DisplayName("addFavorite saves new favorite when validations pass")
    void addFavorite_success() {
        AddFavoriteRequest request = new AddFavoriteRequest("34172");
        UserFavorite savedFavorite = new UserFavorite();
        savedFavorite.setId(new UserFavoriteId(user.getId(), commune.getInseeCode()));
        savedFavorite.setUser(user);
        savedFavorite.setCommune(commune);
        savedFavorite.setCreatedAt(LocalDateTime.of(2024, 2, 10, 10, 0));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(communeRepository.findByInseeCode("34172")).thenReturn(Optional.of(commune));
        when(favoriteRepository.countByUserId(user.getId())).thenReturn(2);
        when(favoriteRepository.existsById(new UserFavoriteId(user.getId(), "34172"))).thenReturn(false);
        when(favoriteRepository.save(any(UserFavorite.class))).thenReturn(savedFavorite);

        UserFavoriteResponse response = userFavoritesService.addFavorite(user.getId(), request);

        assertEquals("34172", response.communeInseeCode());
        assertEquals("Montpellier", response.communeName());
        ArgumentCaptor<UserFavorite> favoriteCaptor = ArgumentCaptor.forClass(UserFavorite.class);
        verify(favoriteRepository).save(favoriteCaptor.capture());
        assertEquals(user, favoriteCaptor.getValue().getUser());
        assertEquals(commune, favoriteCaptor.getValue().getCommune());
    }

    @Test
    @DisplayName("addFavorite throws when user not found")
    void addFavorite_userMissing() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userFavoritesService.addFavorite(user.getId(), new AddFavoriteRequest("34172")));
    }

    @Test
    @DisplayName("addFavorite throws when commune not found")
    void addFavorite_communeMissing() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(communeRepository.findByInseeCode("99999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userFavoritesService.addFavorite(user.getId(), new AddFavoriteRequest("99999")));
    }

    @Test
    @DisplayName("addFavorite throws when limit reached")
    void addFavorite_limitReached() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(communeRepository.findByInseeCode("34172")).thenReturn(Optional.of(commune));
        when(favoriteRepository.countByUserId(user.getId())).thenReturn(10);

        assertThrows(MaximumFavoritesExceededException.class,
                () -> userFavoritesService.addFavorite(user.getId(), new AddFavoriteRequest("34172")));
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("addFavorite throws when duplicate exists")
    void addFavorite_duplicate() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(communeRepository.findByInseeCode("34172")).thenReturn(Optional.of(commune));
        when(favoriteRepository.countByUserId(user.getId())).thenReturn(1);
        when(favoriteRepository.existsById(new UserFavoriteId(user.getId(), "34172"))).thenReturn(true);

        assertThrows(DuplicateFavoriteException.class,
                () -> userFavoritesService.addFavorite(user.getId(), new AddFavoriteRequest("34172")));
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeFavorite deletes when favorite exists")
    void removeFavorite_success() {
        when(communeRepository.findByInseeCode("34172")).thenReturn(Optional.of(commune));
        when(favoriteRepository.existsById(new UserFavoriteId(user.getId(), "34172"))).thenReturn(true);

        userFavoritesService.removeFavorite(user.getId(), "34172");

        verify(favoriteRepository).deleteByUserIdAndCommuneInseeCode(user.getId(), "34172");
    }

    @Test
    @DisplayName("removeFavorite throws when favorite missing")
    void removeFavorite_missing() {
        when(communeRepository.findByInseeCode("34172")).thenReturn(Optional.of(commune));
        when(favoriteRepository.existsById(new UserFavoriteId(user.getId(), "34172"))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userFavoritesService.removeFavorite(user.getId(), "34172"));
        verify(favoriteRepository, never()).deleteByUserIdAndCommuneInseeCode(anyLong(), anyString());
    }

    @Test
    @DisplayName("isFavorited delegates to repository")
    void isFavorited() {
        when(favoriteRepository.existsById_UserIdAndId_CommuneInseeCode(user.getId(), "34172")).thenReturn(true);

        assertTrue(userFavoritesService.isFavorited(user.getId(), "34172"));
    }

    @Test
    @DisplayName("getFavoriteCount returns repository count")
    void getFavoriteCount() {
        when(favoriteRepository.countByUserId(user.getId())).thenReturn(5);

        assertEquals(5, userFavoritesService.getFavoriteCount(user.getId()));
    }

    @Test
    @DisplayName("removeAllFavorites clears when count positive")
    void removeAllFavorites_withFavorites() {
        when(favoriteRepository.countByUserId(user.getId())).thenReturn(2);

        userFavoritesService.removeAllFavorites(user.getId());

        verify(favoriteRepository).deleteAllByUserId(user.getId());
    }

    @Test
    @DisplayName("removeAllFavorites no-ops when count zero")
    void removeAllFavorites_noFavorites() {
        when(favoriteRepository.countByUserId(user.getId())).thenReturn(0);

        userFavoritesService.removeAllFavorites(user.getId());

        verify(favoriteRepository, never()).deleteAllByUserId(anyLong());
    }
}
