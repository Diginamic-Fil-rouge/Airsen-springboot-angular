package fr.airsen.api.service;

import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.dto.ForumVoteDTO;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.User;
import fr.airsen.api.mapper.ForumThreadMapper;
import fr.airsen.api.mapper.ForumVoteMapper;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.ForumVoteRepository;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.security.UserPrincipal;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumVoteServiceTest {

    @Mock
    private ForumVoteRepository forumVoteRepository;

    @Mock
    private ForumVoteMapper forumVoteMapper;

    @Mock
    private ForumThreadMapper forumThreadMapper;

    @Mock
    private ForumThreadRepository forumThreadRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ForumVoteService forumVoteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void testFindAllVoteByThread_Success() {
        ForumThread thread = new ForumThread();
        List<ForumVote> votes = Collections.singletonList(new ForumVote());
        List<ForumVoteDTO> dtos = Collections.singletonList(new ForumVoteDTO());

        when(forumThreadRepository.findById(1L)).thenReturn(Optional.of(thread));
        when(forumVoteRepository.findByThread(thread)).thenReturn(votes);
        when(forumVoteMapper.toDTOs(votes)).thenReturn(dtos);

        List<ForumVoteDTO> result = forumVoteService.findAllVoteByThread(1L);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAllVoteByThread_ThreadNotFound() {
        when(forumThreadRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> forumVoteService.findAllVoteByThread(1L));
        assertEquals("Thread not found", ex.getMessage());
    }

    @Test
    void testFindById_Success() {
        ForumVote vote = new ForumVote();
        ForumVoteDTO dto = new ForumVoteDTO();

        when(forumVoteRepository.findById(1L)).thenReturn(Optional.of(vote));
        when(forumVoteMapper.toDTO(vote)).thenReturn(dto);

        ForumVoteDTO result = forumVoteService.findById(1L);
        assertNotNull(result);
    }

    @Test
    void testFindById_NotFound() {
        when(forumVoteRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> forumVoteService.findById(1L));
        assertEquals("Vote not found", ex.getMessage());
    }

    @Test
    void testVoteThread_Success() {
        ForumThread thread = new ForumThread();
        ForumThread updatedThread = new ForumThread();
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);
        ForumThreadDTO dto = new ForumThreadDTO();

        // Mock Security Context
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(forumThreadRepository.findByIdWithMessages(1L)).thenReturn(Optional.of(thread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(forumVoteRepository.findByUserAndThread(user, thread)).thenReturn(null);
        when(forumVoteRepository.save(any(ForumVote.class))).thenAnswer(i -> i.getArguments()[0]);
        when(forumThreadRepository.findByIdWithMessages(1L)).thenReturn(Optional.of(updatedThread));
        when(forumThreadMapper.toDTO(updatedThread)).thenReturn(dto);

        ForumThreadDTO result = forumVoteService.voteThread(1L, 1);
        assertNotNull(result);

        verify(forumVoteRepository).save(any(ForumVote.class));
        verify(forumThreadMapper).toDTO(updatedThread);
    }

    @Test
    void testVoteThread_ThreadNotFound() {
        when(forumThreadRepository.findByIdWithMessages(1L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> forumVoteService.voteThread(1L, 1));
        assertEquals("Thread not found", ex.getMessage());
    }

    @Test
    void testVoteThread_UserAlreadyVoted() {
        ForumThread thread = new ForumThread();
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);
        ForumVote existingVote = new ForumVote();

        // Mock Security Context
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(forumThreadRepository.findByIdWithMessages(1L)).thenReturn(Optional.of(thread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(forumVoteRepository.findByUserAndThread(user, thread)).thenReturn(existingVote);

        EntityExistsException ex = assertThrows(EntityExistsException.class,
                () -> forumVoteService.voteThread(1L, 1));
        assertEquals("User already voted", ex.getMessage());
    }

    @Test
    void testUnvoteThread_Success() {
        ForumThread thread = new ForumThread();
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);
        ForumVote vote = new ForumVote();

        // Mock Security Context
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(forumThreadRepository.findById(1L)).thenReturn(Optional.of(thread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(forumVoteRepository.findByUserAndThread(user, thread)).thenReturn(vote);

        assertDoesNotThrow(() -> forumVoteService.unvoteThread(1L));
        verify(forumVoteRepository).delete(vote);
    }

    @Test
    void testUnvoteThread_ThreadNotFound() {
        when(forumThreadRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> forumVoteService.unvoteThread(1L));
        assertEquals("Thread not found", ex.getMessage());
    }

    @Test
    void testUnvoteThread_VoteNotFound() {
        ForumThread thread = new ForumThread();
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);

        // Mock Security Context
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(forumThreadRepository.findById(1L)).thenReturn(Optional.of(thread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(forumVoteRepository.findByUserAndThread(user, thread)).thenReturn(null);

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> forumVoteService.unvoteThread(1L));
        assertEquals("Vote not found", ex.getMessage());
    }
}
