package fr.airsen.api.service;

import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.dto.request.ForumThreadCreateRequest;
import fr.airsen.api.dto.request.ForumThreadUpdateRequest;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.mapper.ForumThreadMapper;
import fr.airsen.api.repository.ForumCategoryRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumThreadServiceTest {

    @Mock
    private ForumThreadRepository forumThreadRepository;

    @Mock
    private ForumCategoryRepository forumCategoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ForumThreadMapper mapper;

    @InjectMocks
    private ForumThreadService forumThreadService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAllWithoutFilter() {
        List<ForumThread> threads = Arrays.asList(new ForumThread(), new ForumThread());
        when(forumThreadRepository.findAllWithRelations()).thenReturn(threads);
        when(mapper.toDTOs(threads)).thenReturn(Arrays.asList(new ForumThreadDTO(), new ForumThreadDTO()));

        List<ForumThreadDTO> result = forumThreadService.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void testFindAllWithPaginationAndFilters() {
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);
        ForumCategory category = new ForumCategory();
        category.setId(1L);
        ForumThread forumThread = new ForumThread(user, category, "title", "content", LocalDateTime.now(), LocalDateTime.now(), 0, false, false, 0);
        forumThread.setId(1L);
        Page<ForumThread> page = new PageImpl<>(Collections.singletonList(forumThread));
        Pageable pageable = mock(Pageable.class);

        when(forumThreadRepository.findAll(pageable)).thenReturn(page);

        Page<ForumThreadDTO> result = forumThreadService.findAll(null, null, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindByCategory_Success() {
        ForumCategory category = new ForumCategory();
        List<ForumThread> threads = Collections.singletonList(new ForumThread());
        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(forumThreadRepository.findByCategoryWithMessages(category)).thenReturn(threads);
        when(mapper.toDTOs(threads)).thenReturn(Collections.singletonList(new ForumThreadDTO()));

        List<ForumThreadDTO> result = forumThreadService.findByCategory(1L);
        assertEquals(1, result.size());
    }

    @Test
    void testFindByCategory_NotFound() {
        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> forumThreadService.findByCategory(1L));
        assertEquals("Category not found", ex.getMessage());
    }

    @Test
    void testFindByAuthor_Success() {
        User author = new User();
        List<ForumThread> threads = Collections.singletonList(new ForumThread());
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(forumThreadRepository.findByAuthor(author)).thenReturn(threads);
        when(mapper.toDTOs(threads)).thenReturn(Collections.singletonList(new ForumThreadDTO()));

        List<ForumThreadDTO> result = forumThreadService.findByAuthor(1L);
        assertEquals(1, result.size());
    }

    @Test
    void testFindByAuthor_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> forumThreadService.findByAuthor(1L));
        assertEquals("Author not found", ex.getMessage());
    }

    @Test
    void testFindById_IncrementsViewCount() {
        ForumThread thread = new ForumThread();
        thread.setViewCount(5);
        ForumThreadDTO dto = new ForumThreadDTO();

        when(forumThreadRepository.findByIdWithMessages(1L)).thenReturn(Optional.of(thread));
        when(mapper.toDTO(thread)).thenReturn(dto);
        when(forumThreadRepository.save(thread)).thenReturn(thread);

        ForumThreadDTO result = forumThreadService.findById(1L);
        assertNotNull(result);
        assertEquals(6, thread.getViewCount());
        verify(forumThreadRepository).save(thread);
    }

    @Test
    void testCreateThread_Success() {
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);
        ForumCategory category = new ForumCategory();

        ForumThreadCreateRequest request = new ForumThreadCreateRequest();
        request.setTitle("Title");
        request.setContent("Content");
        request.setCategoryId(1L);

        // Mock Security Context
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(forumThreadRepository.save(any(ForumThread.class))).thenAnswer(i -> i.getArguments()[0]);

        ForumThreadDTO dto = forumThreadService.createThread(request);
        assertEquals("Title", dto.getTitle());
    }

    @Test
    void testCreateThread_UserNotFound() {
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);
        ForumThreadCreateRequest request = new ForumThreadCreateRequest();
        request.setCategoryId(1L);

        // Mock Security Context
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> forumThreadService.createThread(request));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void testAddThreadToCategory_Success() {
        User user = new User("test@test.com", "test", "test", "test");
        user.setId(1L);
        ForumCategory category = new ForumCategory();
        category.setThreads(Collections.emptyList());

        ForumThreadCreateRequest request = new ForumThreadCreateRequest();
        request.setTitle("Title");
        request.setContent("Content");

        // Mock Security Context
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(forumThreadRepository.findAll()).thenReturn(Collections.singletonList(new ForumThread()));
        when(mapper.toDTOs(anyList())).thenReturn(Collections.singletonList(new ForumThreadDTO()));

        List<ForumThreadDTO> result = forumThreadService.addThreadToCategory(1L, request);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateThread_Success() {
        ForumThread thread = new ForumThread();
        thread.setTitle("Old");
        thread.setContent("Old");
        ForumCategory newCategory = new ForumCategory();

        ForumThreadUpdateRequest request = new ForumThreadUpdateRequest();
        request.setTitle("New");
        request.setContent("New");
        request.setCategoryId(1L);

        when(forumThreadRepository.findById(1L)).thenReturn(Optional.of(thread));
        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(newCategory));
        when(forumThreadRepository.save(thread)).thenReturn(thread);

        ForumThreadDTO dto = forumThreadService.updateThread(1L, request);
        assertEquals("New", thread.getTitle());
        assertEquals("New", thread.getContent());
        assertEquals(newCategory, thread.getCategory());
    }

    @Test
    void testDeleteThread_Success() {
        ForumThread thread = new ForumThread();
        User author = new User();
        author.setThreads(new ArrayList<>(List.of(thread)));
        ForumCategory category = new ForumCategory();
        category.setThreads(new ArrayList<>(List.of(thread)));
        thread.setAuthor(author);
        thread.setCategory(category);

        when(forumThreadRepository.findById(1L)).thenReturn(Optional.of(thread));

        forumThreadService.deleteThread(1L);

        verify(forumThreadRepository).delete(thread);
        assertFalse(author.getThreads().contains(thread));
        assertFalse(category.getThreads().contains(thread));
    }

    @Test
    void testDeleteThread_NotFound() {
        when(forumThreadRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> forumThreadService.deleteThread(1L));
        assertEquals("Thread not found", ex.getMessage());
    }
}
