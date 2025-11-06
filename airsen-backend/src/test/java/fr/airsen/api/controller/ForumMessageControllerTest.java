package fr.airsen.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.dto.request.ForumMessageUpdateRequest;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.service.ForumMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ForumMessageController.
 */
@WebMvcTest(ForumMessageController.class)
class ForumMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForumMessageService forumMessageService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDTO userDTO;
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

        messageDTO = new ForumMessageDTO();
        messageDTO.setId(1L);
        messageDTO.setAuthor(userDTO);
        messageDTO.setContent("Hello world");
        messageDTO.setCreatedDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /forum/messages - should return list of messages")
    void testGetAllMessages() throws Exception {
        when(forumMessageService.findAll()).thenReturn(List.of(messageDTO));

        mockMvc.perform(get("/forum/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].author").value(userDTO))
                .andExpect(jsonPath("$[0].content").value("Hello world"));
    }

    @Test
    @DisplayName("GET /forum/messages/{id} - should return message by ID")
    void testGetMessageById() throws Exception {
        when(forumMessageService.findById(1L)).thenReturn(messageDTO);

        mockMvc.perform(get("/forum/messages/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.author").value(userDTO))
                .andExpect(jsonPath("$.content").value("Hello world"));
    }

    @Test
    @DisplayName("PUT /forum/messages/{id} - should update message and return 200")
    void testUpdateMessage() throws Exception {
        ForumMessageUpdateRequest request = new ForumMessageUpdateRequest();
        request.setContent("Updated content");

        when(forumMessageService.updateMessage(eq(1L), any())).thenReturn(messageDTO);

        mockMvc.perform(put("/forum/messages/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("Hello world")); // returned mock data
    }

    @Test
    @DisplayName("DELETE /forum/messages/{id} - should delete message and return 204")
    void testDeleteMessage() throws Exception {
        doNothing().when(forumMessageService).deleteMessage(1L);

        mockMvc.perform(delete("/forum/messages/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
