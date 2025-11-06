package fr.airsen.api.service;

import fr.airsen.api.dto.ForumCategoryDTO;
import fr.airsen.api.dto.request.ForumCategoryCreateRequest;
import fr.airsen.api.dto.request.ForumCategoryUpdateRequest;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.mapper.ForumCategoryMapper;
import fr.airsen.api.repository.ForumCategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForumCategoryServiceTest {

    @Mock
    private ForumCategoryRepository forumCategoryRepository;

    @Mock
    private ForumCategoryMapper mapper;

    @InjectMocks
    private ForumCategoryService forumCategoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        List<ForumCategory> categories = Arrays.asList(new ForumCategory(), new ForumCategory());
        List<ForumCategoryDTO> dtos = Arrays.asList(new ForumCategoryDTO(), new ForumCategoryDTO());

        when(forumCategoryRepository.findAll()).thenReturn(categories);
        when(mapper.toDTOs(categories)).thenReturn(dtos);

        List<ForumCategoryDTO> result = forumCategoryService.findAll();
        assertEquals(2, result.size());
        verify(forumCategoryRepository, times(1)).findAll();
        verify(mapper, times(1)).toDTOs(categories);
    }

    @Test
    void testFindById_Success() {
        ForumCategory category = new ForumCategory();
        category.setId(1L);
        ForumCategoryDTO dto = new ForumCategoryDTO();

        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mapper.toDTO(category)).thenReturn(dto);

        ForumCategoryDTO result = forumCategoryService.findById(1L);
        assertNotNull(result);
        verify(forumCategoryRepository).findById(1L);
        verify(mapper).toDTO(category);
    }

    @Test
    void testFindById_NotFound() {
        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> forumCategoryService.findById(1L));
        assertEquals("Category not found", exception.getMessage());
    }

    @Test
    void testAddForumCategory_Success() {
        ForumCategoryCreateRequest request = new ForumCategoryCreateRequest();
        request.setName("New Category");
        request.setDescription("Description");
        request.setColor("Blue");

        when(forumCategoryRepository.findByName("New Category")).thenReturn(null);
        when(forumCategoryRepository.findAll()).thenReturn(Arrays.asList(new ForumCategory()));
        when(mapper.toDTOs(anyList())).thenReturn(Arrays.asList(new ForumCategoryDTO()));

        List<ForumCategoryDTO> result = forumCategoryService.addForumCategory(request);
        assertEquals(1, result.size());

        ArgumentCaptor<ForumCategory> captor = ArgumentCaptor.forClass(ForumCategory.class);
        verify(forumCategoryRepository).save(captor.capture());
        assertEquals("New Category", captor.getValue().getName());
        assertEquals("Description", captor.getValue().getDescription());
        assertEquals("Blue", captor.getValue().getColor());
    }

    @Test
    void testAddForumCategory_AlreadyExists() {
        ForumCategoryCreateRequest request = new ForumCategoryCreateRequest();
        request.setName("Existing Category");

        when(forumCategoryRepository.findByName("Existing Category")).thenReturn(new ForumCategory());
        EntityExistsException exception = assertThrows(EntityExistsException.class,
                () -> forumCategoryService.addForumCategory(request));
        assertEquals("Failed to add category - Category with same name already exists", exception.getMessage());
    }

    @Test
    void testEditForumCategory_Success() {
        ForumCategory category = new ForumCategory();
        category.setId(1L);
        ForumCategoryUpdateRequest request = new ForumCategoryUpdateRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");

        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(forumCategoryRepository.findByName("Updated Name")).thenReturn(null);
        when(forumCategoryRepository.findAll()).thenReturn(Arrays.asList(category));
        when(mapper.toDTOs(anyList())).thenReturn(Arrays.asList(new ForumCategoryDTO()));

        List<ForumCategoryDTO> result = forumCategoryService.editForumCategory(1L, request);
        assertEquals(1, result.size());
        assertEquals("Updated Name", category.getName());
        assertEquals("Updated Description", category.getDescription());
    }

    @Test
    void testEditForumCategory_NotFound() {
        ForumCategoryUpdateRequest request = new ForumCategoryUpdateRequest();
        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> forumCategoryService.editForumCategory(1L, request));
        assertTrue(exception.getMessage().contains("Failed to update category - Category not found"));
    }

    @Test
    void testEditForumCategory_NameConflict() {
        ForumCategory category = new ForumCategory();
        category.setId(1L);

        ForumCategory anotherCategory = new ForumCategory();
        anotherCategory.setId(2L);

        ForumCategoryUpdateRequest request = new ForumCategoryUpdateRequest();
        request.setName("Conflict Name");

        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(forumCategoryRepository.findByName("Conflict Name")).thenReturn(anotherCategory);

        EntityExistsException exception = assertThrows(EntityExistsException.class,
                () -> forumCategoryService.editForumCategory(1L, request));
        assertTrue(exception.getMessage().contains("Failed to update category - Category with same name already exists"));
    }

    @Test
    void testDeleteForumCategory_Success() {
        ForumCategory category = new ForumCategory();
        category.setId(1L);

        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        doNothing().when(forumCategoryRepository).deleteById(1L);

        assertDoesNotThrow(() -> forumCategoryService.deleteForumCategory(1L));
        verify(forumCategoryRepository).deleteById(1L);
    }

    @Test
    void testDeleteForumCategory_NotFound() {
        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> forumCategoryService.deleteForumCategory(1L));
        assertEquals("Failed to delete category - Category not found", exception.getMessage());
    }
}
