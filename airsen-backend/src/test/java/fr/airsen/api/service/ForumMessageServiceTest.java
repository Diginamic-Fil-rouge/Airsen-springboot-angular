package fr.airsen.api.service;

import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.dto.request.ForumMessageCreateRequest;
import fr.airsen.api.dto.request.ForumMessageUpdateRequest;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.mapper.ForumMessageMapper;
import fr.airsen.api.repository.ForumMessageRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.security.UserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumMessageServiceTest {

    @Mock
    private ForumMessageRepository forumMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ForumThreadRepository forumThreadRepository;

    @Mock
    private ForumMessageMapper forumMessageMapper;

    @InjectMocks
    private ForumMessageService forumMessageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        List<ForumMessage> messages = Arrays.asList(new ForumMessage(), new ForumMessage());
        List<ForumMessageDTO> dtos = Arrays.asList(new ForumMessageDTO(), new ForumMessageDTO());

        when(forumMessageRepository.findAll()).thenReturn(messages);
        when(forumMessageMapper.toDTOs(messages)).thenReturn(dtos);

        List<ForumMessageDTO> result = forumMessageService.findAll();
        assertEquals(2, result.size());
        verify(forumMessageRepository).findAll();
        verify(forumMessageMapper).toDTOs(messages);
    }

    @Test
    void testFindById_Found() {
        ForumMessage message = new ForumMessage();
        ForumMessageDTO dto = new ForumMessageDTO();

        when(forumMessageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(forumMessageMapper.toDTO(message)).thenReturn(dto);

        ForumMessageDTO result = forumMessageService.findById(1L);
        assertNotNull(result);
        verify(forumMessageRepository).findById(1L);
        verify(forumMessageMapper).toDTO(message);
    }

    @Test
    void testFindById_NotFound() {
        when(forumMessageRepository.findById(1L)).thenReturn(Optional.empty());
        ForumMessageDTO result = forumMessageService.findById(1L);
        assertNull(result);
    }

    @Test
    void testFindByAuthor_Success() {
        User user = new User();
        List<ForumMessage> messages = Collections.singletonList(new ForumMessage());
        List<ForumMessageDTO> dtos = Collections.singletonList(new ForumMessageDTO());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(forumMessageRepository.findByAuthor(user)).thenReturn(messages);
        when(forumMessageMapper.toDTOs(messages)).thenReturn(dtos);

        List<ForumMessageDTO> result = forumMessageService.findByAuthor(1L);
        assertEquals(1, result.size());
        verify(forumMessageRepository).findByAuthor(user);
    }

    @Test
    void testFindByAuthor_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> forumMessageService.findByAuthor(1L));
        assertEquals("Author not found", exception.getMessage());
    }

    @Test
    void testGetMessagesByThread_Success() {
        ForumThread thread = new ForumThread();
        List<ForumMessage> messages = Collections.singletonList(new ForumMessage());
        List<ForumMessageDTO> dtos = Collections.singletonList(new ForumMessageDTO());

        when(forumThreadRepository.findById(1L)).thenReturn(Optional.of(thread));
        when(forumMessageRepository.findByThread(thread)).thenReturn(messages);
        when(forumMessageMapper.toDTOs(messages)).thenReturn(dtos);

        List<ForumMessageDTO> result = forumMessageService.getMessagesByThread(1L);
        assertEquals(1, result.size());
        verify(forumMessageRepository).findByThread(thread);
    }

    @Test
    void testGetMessagesByThread_NotFound() {
        when(forumThreadRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> forumMessageService.getMessagesByThread(1L));
        assertEquals("Thread not found", exception.getMessage());
    }

    @Test
    void testAddMessageToThread_Success() {
        // Mock thread and user
        ForumThread thread = new ForumThread();
        thread.setMessages(Collections.emptyList());
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);

        ForumMessageCreateRequest request = new ForumMessageCreateRequest();
        request.setContent("Hello World");

        ForumMessage savedMessage = new ForumMessage();
        ForumMessageDTO dto = new ForumMessageDTO();

        // Mock Security Context
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(forumThreadRepository.findById(1L)).thenReturn(Optional.of(thread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(forumMessageRepository.save(any(ForumMessage.class))).thenReturn(savedMessage);
        when(forumThreadRepository.save(thread)).thenReturn(thread);
        when(forumMessageMapper.toDTO(savedMessage)).thenReturn(dto);

        ForumMessageDTO result = forumMessageService.addMessageToThread(1L, request);
        assertNotNull(result);

        ArgumentCaptor<ForumMessage> messageCaptor = ArgumentCaptor.forClass(ForumMessage.class);
        verify(forumMessageRepository).save(messageCaptor.capture());
        assertEquals("Hello World", messageCaptor.getValue().getContent());
        assertEquals(user, messageCaptor.getValue().getAuthor());
        assertEquals(thread, messageCaptor.getValue().getThread());
    }

    @Test
    void testAddMessageToThread_ThreadNotFound() {
        ForumMessageCreateRequest request = new ForumMessageCreateRequest();
        when(forumThreadRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> forumMessageService.addMessageToThread(1L, request));
        assertTrue(exception.getMessage().contains("Thread not found with ID: 1"));
    }

    @Test
    void testUpdateMessage_Success() {
        ForumMessage message = new ForumMessage();
        message.setContent("Old content");

        ForumMessageUpdateRequest request = new ForumMessageUpdateRequest();
        request.setContent("New content");

        ForumMessage updatedMessage = new ForumMessage();
        updatedMessage.setContent("New content");

        ForumMessageDTO dto = new ForumMessageDTO();

        when(forumMessageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(forumMessageRepository.save(message)).thenReturn(updatedMessage);
        when(forumMessageMapper.toDTO(updatedMessage)).thenReturn(dto);

        ForumMessageDTO result = forumMessageService.updateMessage(1L, request);
        assertNotNull(result);
        assertEquals("New content", message.getContent());
    }

    @Test
    void testUpdateMessage_NotFound() {
        ForumMessageUpdateRequest request = new ForumMessageUpdateRequest();
        when(forumMessageRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> forumMessageService.updateMessage(1L, request));
        assertTrue(exception.getMessage().contains("Message not found with ID: 1"));
    }

    @Test
    void testDeleteMessage_Success() {
        ForumMessage message = new ForumMessage();
        User author = new User();
        author.setMessages(new ArrayList<>(List.of(message)));
        ForumThread thread = new ForumThread();
        thread.setMessages(new ArrayList<>(List.of(message)));
        message.setAuthor(author);
        message.setThread(thread);

        when(forumMessageRepository.findById(1L)).thenReturn(Optional.of(message));

        forumMessageService.deleteMessage(1L);

        verify(forumMessageRepository).deleteById(1L);
        assertFalse(author.getMessages().contains(message));
        assertFalse(thread.getMessages().contains(message));
    }


    @Test
    void testDeleteMessage_NotFound() {
        when(forumMessageRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> forumMessageService.deleteMessage(1L));
        assertEquals("Message not found", exception.getMessage());
    }
}
