package fr.airsen.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.dto.request.ForumMessageCreateRequest;
import fr.airsen.api.dto.request.ForumThreadCreateRequest;
import fr.airsen.api.dto.request.ForumThreadUpdateRequest;
import fr.airsen.api.dto.request.VoteRequest;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.entity.enums.VoteType;
import fr.airsen.api.security.UserPrincipal;
import fr.airsen.api.service.ForumMessageService;
import fr.airsen.api.service.ForumThreadService;
import fr.airsen.api.service.ForumVoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ForumThreadController.
 */
@WebMvcTest(ForumThreadController.class)
class ForumThreadControllerTest {
    private final Long userId = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForumThreadService forumThreadService;

    @MockBean
    private ForumMessageService forumMessageService;

    @MockBean
    private ForumVoteService forumVoteService;

    @MockBean
    private fr.airsen.api.security.JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDTO userDTO;
    private ForumThreadDTO threadDTO;
    private ForumMessageDTO messageDTO;

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");
        userDTO.setEmail("john.doe@example.com");
        userDTO.setRole(UserRole.USER);
        userDTO.setCreatedAt(LocalDateTime.now());

        threadDTO = new ForumThreadDTO();
        threadDTO.setId(1L);
        threadDTO.setTitle("Air Quality Discussion");
        threadDTO.setLikeCount(5);
        threadDTO.setCreatedDate(LocalDateTime.now());

        messageDTO = new ForumMessageDTO();
        messageDTO.setId(10L);
        messageDTO.setContent("Great topic!");
        messageDTO.setAuthor(userDTO);
        messageDTO.setCreatedDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /forum/threads - should return paginated list of threads")
    void testGetAllThreads() throws Exception {
        Page<ForumThreadDTO> page = new PageImpl<>(List.of(threadDTO));
        when(forumThreadService.findAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/forum/threads")
                        .param("page", "0")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Air Quality Discussion"));
    }

    @Test
    @DisplayName("POST /forum/threads - should create thread and return 201")
    void testCreateThread() throws Exception {
        ForumThreadCreateRequest request = new ForumThreadCreateRequest();
        request.setTitle("New Thread");
        request.setContent("Let's talk about air quality");
        request.setCategoryId(2L);

        when(forumThreadService.createThread(any())).thenReturn(threadDTO);

        mockMvc.perform(post("/forum/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Air Quality Discussion"));
    }

    @Test
    @DisplayName("GET /forum/threads/{id} - should return thread by ID")
    void testGetThreadById() throws Exception {
        when(forumThreadService.findById(1L)).thenReturn(threadDTO);

        mockMvc.perform(get("/forum/threads/{id}", 1L)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Air Quality Discussion"));
    }

    @Test
    @DisplayName("GET /forum/threads/{id}/messages - should return list of messages in thread")
    void testGetMessagesByThread() throws Exception {
        when(forumMessageService.getMessagesByThread(1L)).thenReturn(List.of(messageDTO));

        mockMvc.perform(get("/forum/threads/{id}/messages", 1L)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].content").value("Great topic!"))
                .andExpect(jsonPath("$[0].author").value(userDTO));
    }

    @Test
    @DisplayName("POST /forum/threads/{id}/messages - should create message in thread")
    void testAddMessageToThread() throws Exception {
        ForumMessageCreateRequest request = new ForumMessageCreateRequest();
        request.setContent("Nice discussion!");
        when(forumMessageService.addMessageToThread(eq(1L), any())).thenReturn(messageDTO);

        mockMvc.perform(post("/forum/threads/{id}/messages", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.content").value("Great topic!"));
    }

    @Test
    @DisplayName("PUT /forum/threads/{id} - should update thread")
    void testUpdateThread() throws Exception {
        ForumThreadUpdateRequest request = new ForumThreadUpdateRequest();
        request.setTitle("Updated Thread");

        when(forumThreadService.updateThread(eq(1L), any())).thenReturn(threadDTO);

        mockMvc.perform(put("/forum/threads/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Air Quality Discussion"));
    }

    @Test
    @DisplayName("DELETE /forum/threads/{id} - should delete thread and return 204")
    void testDeleteThread() throws Exception {
        doNothing().when(forumThreadService).deleteThread(1L);

        mockMvc.perform(delete("/forum/threads/{id}", 1L)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.ADMIN))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /forum/threads/{id}/vote - should vote on thread")
    void testVoteThread() throws Exception {
        VoteRequest voteRequest = new VoteRequest(VoteType.LIKE);

        when(forumVoteService.voteThread(eq(1L), eq(VoteType.LIKE.toInt()))).thenReturn(threadDTO);

        mockMvc.perform(post("/forum/threads/{id}/vote", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.likeCount").value(5));
    }

    @Test
    @DisplayName("DELETE /forum/threads/{id}/vote - should unvote thread and return 204")
    void testUnvoteThread() throws Exception {
        doNothing().when(forumVoteService).unvoteThread(1L);

        mockMvc.perform(delete("/forum/threads/{id}/vote", 1L)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuthentication(userId, UserRole.USER))))
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
