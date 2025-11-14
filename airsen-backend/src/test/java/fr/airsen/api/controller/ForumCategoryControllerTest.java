package fr.airsen.api.controller;

import fr.airsen.api.dto.ForumCategoryDTO;
import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.dto.request.ForumCategoryCreateRequest;
import fr.airsen.api.dto.request.ForumCategoryUpdateRequest;
import fr.airsen.api.dto.request.ForumThreadCreateRequest;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.security.UserPrincipal;
import fr.airsen.api.service.ForumCategoryService;
import fr.airsen.api.service.ForumThreadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ForumCategoryController.
 */
@WebMvcTest(ForumCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class ForumCategoryControllerTest {
    private final  Long userId = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForumCategoryService categoryService;

    @MockBean
    private ForumThreadService threadService;

    @MockBean
    private fr.airsen.api.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private fr.airsen.api.service.JwtBlacklistService jwtBlacklistService;

    @Autowired
    private ObjectMapper objectMapper;


    private ForumCategoryDTO categoryDTO;
    private ForumThreadDTO threadDTO;

    @BeforeEach
    void setUp() {
        categoryDTO = new ForumCategoryDTO();
        categoryDTO.setId(1L);
        categoryDTO.setName("General");
        categoryDTO.setDescription("General discussions");

        threadDTO = new ForumThreadDTO();
        threadDTO.setId(1L);
        threadDTO.setTitle("Welcome thread");
        threadDTO.setLastMessageDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /forum/categories - should return list of categories")
    void testListCategories() throws Exception {
        List<ForumCategoryDTO> categories = List.of(categoryDTO);
        when(categoryService.findAll()).thenReturn(categories);

        mockMvc.perform(get("/forum/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(categoryDTO.getId()))
                .andExpect(jsonPath("$[0].name").value(categoryDTO.getName()));
    }

    @Test
    @DisplayName("GET /forum/categories/{id} - should return category by ID")
    void testGetCategoryById() throws Exception {
        when(categoryService.findById(1L)).thenReturn(categoryDTO);

        mockMvc.perform(get("/forum/categories/{id}", 1L)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("General"));
    }

    @Test
    @DisplayName("GET /forum/categories/{id}/threads - should return threads by category")
    void testListThreadsByCategory() throws Exception {
        when(threadService.findByCategory(1L)).thenReturn(new ArrayList<>(List.of(threadDTO)));

        mockMvc.perform(get("/forum/categories/{id}/threads", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andDo(System.out::println)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(threadDTO.getId()))
                .andExpect(jsonPath("$[0].title").value("Welcome thread"));
    }

    @Test
    @DisplayName("POST /forum/categories/{id}/threads - should create thread and return 201")
    void testAddThread() throws Exception {
        ForumThreadCreateRequest request = new ForumThreadCreateRequest();
        request.setTitle("New Topic");
        request.setContent("Let's talk about air quality");
        request.setCategoryId(1L);

        when(threadService.addThreadToCategory(eq(1L), any())).thenReturn(List.of(threadDTO));

        mockMvc.perform(post("/forum/categories/{id}/threads", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(threadDTO.getId()));
    }

    @Test
    @DisplayName("POST /forum/categories - should create category and return 201")
    void testAddCategory() throws Exception {
        ForumCategoryCreateRequest request = new ForumCategoryCreateRequest();
        request.setName("New Category");
        request.setDescription("New Category Description");
        request.setColor("#FF0000");

        when(categoryService.addForumCategory(any())).thenReturn(List.of(categoryDTO));

        mockMvc.perform(post("/forum/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.ADMIN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("General"));
    }

    @Test
    @DisplayName("PUT /forum/categories/{id} - should update category")
    void testEditCategory() throws Exception {
        ForumCategoryUpdateRequest request = new ForumCategoryUpdateRequest();
        request.setName("Updated Name");

        when(categoryService.editForumCategory(eq(1L), any())).thenReturn(List.of(categoryDTO));

        mockMvc.perform(put("/forum/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(categoryDTO.getId()));
    }

    @Test
    @DisplayName("DELETE /forum/categories/{id} - should delete category and return 204")
    void testDeleteCategory() throws Exception {
        doNothing().when(categoryService).deleteForumCategory(1L);

        mockMvc.perform(delete("/forum/categories/{id}", 1L)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.ADMIN))))
                .andExpect(status().isNoContent());
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
