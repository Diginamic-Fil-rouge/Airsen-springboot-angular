package fr.airsen.api.controller;

import fr.airsen.api.dto.response.UserFavoriteResponse;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.exception.GlobalExceptionHandler;
import fr.airsen.api.exception.MaximumFavoritesExceededException;
import fr.airsen.api.exception.ResourceNotFoundException;
import fr.airsen.api.security.UserPrincipal;
import fr.airsen.api.service.UserFavoritesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserFavoritesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserFavoritesController tests")
class UserFavoritesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserFavoritesService favoritesService;

    @Test
    @DisplayName("GET /users/{id}/favorites returns list when access granted")
    void getUserFavorites_success() throws Exception {
        Long userId = 1L;
        UserFavoriteResponse response = new UserFavoriteResponse(
                "34172",
                "Montpellier",
                "Herault",
                "Occitanie",
                LocalDateTime.of(2024, 1, 1, 12, 0)
        );

        when(favoritesService.getUserFavorites(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/users/{userId}/favorites", userId)
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].communeInseeCode").value("34172"))
            .andExpect(jsonPath("$[0].communeName").value("Montpellier"))
            .andExpect(jsonPath("$[0].departmentName").value("Herault"))
            .andExpect(jsonPath("$[0].regionName").value("Occitanie"));

        verify(favoritesService).getUserFavorites(userId);
    }

    @Test
    @DisplayName("GET /users/{id}/favorites returns 403 when user id mismatch")
    void getUserFavorites_forbidden() throws Exception {
        mockMvc.perform(get("/users/{userId}/favorites", 1L)
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(2L, UserRole.USER))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message", containsString("only access your own favorites")));

        verify(favoritesService, never()).getUserFavorites(anyLong());
    }

    @Test
    @DisplayName("POST /users/{id}/favorites creates favorite and returns 201")
    void addFavorite_success() throws Exception {
        Long userId = 3L;
        String requestJson = """
            {
              "communeInseeCode": "75056"
            }
            """;

        UserFavoriteResponse response = new UserFavoriteResponse(
                "75056",
                "Paris",
                "Paris",
                "Ile-de-France",
                LocalDateTime.of(2024, 2, 10, 10, 0)
        );

        when(favoritesService.addFavorite(eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/users/{userId}/favorites", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.communeInseeCode").value("75056"))
            .andExpect(jsonPath("$.communeName").value("Paris"))
            .andExpect(jsonPath("$.departmentName").value("Paris"))
            .andExpect(jsonPath("$.regionName").value("Ile-de-France"));

        verify(favoritesService).addFavorite(eq(userId), any());
    }

    @Test
    @DisplayName("POST /users/{id}/favorites returns 400 when limit exceeded")
    void addFavorite_limitExceeded() throws Exception {
        Long userId = 4L;
        when(favoritesService.addFavorite(eq(userId), any()))
                .thenThrow(new MaximumFavoritesExceededException("Maximum favorites reached"));

        mockMvc.perform(post("/users/{userId}/favorites", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "communeInseeCode": "12345" }
                    """)
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MAXIMUM_FAVORITES_EXCEEDED"))
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /users/{id}/favorites/{commune} returns 204 on success")
    void removeFavorite_success() throws Exception {
        mockMvc.perform(delete("/users/{userId}/favorites/{code}", 5L, "34172")
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(5L, UserRole.USER))))
            .andExpect(status().isNoContent());

        verify(favoritesService).removeFavorite(5L, "34172");
    }

    @Test
    @DisplayName("DELETE /users/{id}/favorites/{commune} returns 404 when favorite missing")
    void removeFavorite_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("Favorite not found"))
                .when(favoritesService).removeFavorite(6L, "99999");

        mockMvc.perform(delete("/users/{userId}/favorites/{code}", 6L, "99999")
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(6L, UserRole.USER))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /users/{id}/favorites/{commune}/check returns boolean flag")
    void checkFavorite_success() throws Exception {
        when(favoritesService.isFavorited(7L, "34172")).thenReturn(true);

        mockMvc.perform(get("/users/{userId}/favorites/{code}/check", 7L, "34172")
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(7L, UserRole.USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isFavorited").value(true));

        verify(favoritesService).isFavorited(7L, "34172");
    }

    @Test
    @DisplayName("GET /users/{id}/favorites/count returns count and maximum")
    void getFavoriteCount_success() throws Exception {
        when(favoritesService.getFavoriteCount(8L)).thenReturn(4);

        mockMvc.perform(get("/users/{userId}/favorites/count", 8L)
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(8L, UserRole.USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(4))
            .andExpect(jsonPath("$.maximum").value(10));

        verify(favoritesService).getFavoriteCount(8L);
    }

    @Test
    @DisplayName("Admin principal can access other user's favorites")
    void getUserFavorites_adminAccess() throws Exception {
        Long targetUserId = 9L;
        when(favoritesService.getUserFavorites(targetUserId)).thenReturn(List.of());

        mockMvc.perform(get("/users/{userId}/favorites", targetUserId)
                .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(100L, UserRole.ADMIN))))
            .andExpect(status().isOk());

        verify(favoritesService).getUserFavorites(targetUserId);
    }

    private UsernamePasswordAuthenticationToken userAuthentication(Long userId, UserRole role) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user" + userId + "@example.com");
        user.setPassword("secret");
        user.setRole(role);

        UserPrincipal principal = UserPrincipal.create(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
