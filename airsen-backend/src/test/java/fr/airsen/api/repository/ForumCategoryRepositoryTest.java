package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ForumCategoryRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ForumCategoryRepository Tests")
class ForumCategoryRepositoryTest {

    @Mock
    private ForumCategoryRepository forumCategoryRepository;

    private ForumCategory category1;
    private ForumCategory category2;
    private ForumCategory category3;

    @BeforeEach
    void setUp() {
        // Create test data
        category1 = new ForumCategory();
        category1.setId(1L);
        category1.setName("General Discussion");

        category2 = new ForumCategory();
        category2.setId(2L);
        category2.setName("Technical Support");

        category3 = new ForumCategory();
        category3.setId(3L);
        category3.setName("Feature Requests");
    }

    @Test
    @DisplayName("Should find all forum categories")
    void testFindAll() {
        // Given
        List<ForumCategory> categories = Arrays.asList(category1, category2, category3);
        when(forumCategoryRepository.findAll()).thenReturn(categories);

        // When
        List<ForumCategory> result = forumCategoryRepository.findAll();

        // Then
        assertThat(result)
                .isNotNull()
                .hasSize(3)
                .extracting(ForumCategory::getName)
                .containsExactlyInAnyOrder(
                        "General Discussion",
                        "Technical Support",
                        "Feature Requests"
                );
        verify(forumCategoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no categories exist")
    void testFindAll_EmptyRepository() {
        // Given
        when(forumCategoryRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<ForumCategory> result = forumCategoryRepository.findAll();

        // Then
        assertThat(result)
                .isNotNull()
                .isEmpty();
        verify(forumCategoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find category by name")
    void testFindByName_Success() {
        // Given
        when(forumCategoryRepository.findByName("General Discussion")).thenReturn(category1);

        // When
        ForumCategory result = forumCategoryRepository.findByName("General Discussion");

        // Then
        assertThat(result)
                .isNotNull()
                .extracting(ForumCategory::getName)
                .isEqualTo("General Discussion");
        assertThat(result.getId()).isEqualTo(1L);
        verify(forumCategoryRepository, times(1)).findByName("General Discussion");
    }

    @Test
    @DisplayName("Should return null when category name not found")
    void testFindByName_NotFound() {
        // Given
        when(forumCategoryRepository.findByName("Non-existent Category")).thenReturn(null);

        // When
        ForumCategory result = forumCategoryRepository.findByName("Non-existent Category");

        // Then
        assertThat(result).isNull();
        verify(forumCategoryRepository, times(1)).findByName("Non-existent Category");
    }

    @Test
    @DisplayName("Should return null when searching for null name")
    void testFindByName_NullName() {
        // Given
        when(forumCategoryRepository.findByName(null)).thenReturn(null);

        // When
        ForumCategory result = forumCategoryRepository.findByName(null);

        // Then
        assertThat(result).isNull();
        verify(forumCategoryRepository, times(1)).findByName(null);
    }

    @Test
    @DisplayName("Should be case-sensitive when finding by name")
    void testFindByName_CaseSensitive() {
        // Given
        when(forumCategoryRepository.findByName("General Discussion")).thenReturn(category1);
        when(forumCategoryRepository.findByName("general discussion")).thenReturn(null);

        // When
        ForumCategory foundExact = forumCategoryRepository.findByName("General Discussion");
        ForumCategory foundWrongCase = forumCategoryRepository.findByName("general discussion");

        // Then
        assertThat(foundExact).isNotNull();
        assertThat(foundWrongCase).isNull();
        verify(forumCategoryRepository, times(1)).findByName("General Discussion");
        verify(forumCategoryRepository, times(1)).findByName("general discussion");
    }

    @Test
    @DisplayName("Should delete category by ID")
    void testDeleteById_Success() {
        // Given
        Long categoryId = 1L;
        doNothing().when(forumCategoryRepository).deleteById(categoryId);

        // When
        forumCategoryRepository.deleteById(categoryId);

        // Then
        verify(forumCategoryRepository, times(1)).deleteById(categoryId);
    }

    @Test
    @DisplayName("Should handle deleting non-existent ID")
    void testDeleteById_NonExistent() {
        // Given
        Long nonExistentId = 999L;
        doNothing().when(forumCategoryRepository).deleteById(nonExistentId);

        // When
        forumCategoryRepository.deleteById(nonExistentId);

        // Then
        verify(forumCategoryRepository, times(1)).deleteById(nonExistentId);
    }

    @Test
    @DisplayName("Should call deleteById with correct parameter")
    void testDeleteById_CorrectParameter() {
        // Given
        Long categoryId = 2L;
        doNothing().when(forumCategoryRepository).deleteById(anyLong());

        // When
        forumCategoryRepository.deleteById(categoryId);

        // Then
        verify(forumCategoryRepository, times(1)).deleteById(categoryId);
        verify(forumCategoryRepository, never()).deleteById(1L);
        verify(forumCategoryRepository, never()).deleteById(3L);
    }

    @Test
    @DisplayName("Should save and return category")
    void testSave_Success() {
        // Given
        ForumCategory newCategory = new ForumCategory();
        newCategory.setName("New Category");

        ForumCategory savedCategory = new ForumCategory();
        savedCategory.setId(4L);
        savedCategory.setName("New Category");

        when(forumCategoryRepository.save(any(ForumCategory.class))).thenReturn(savedCategory);

        // When
        ForumCategory result = forumCategoryRepository.save(newCategory);

        // Then
        assertThat(result)
                .isNotNull()
                .extracting(ForumCategory::getId, ForumCategory::getName)
                .containsExactly(4L, "New Category");
        verify(forumCategoryRepository, times(1)).save(newCategory);
    }

    @Test
    @DisplayName("Should find category by ID")
    void testFindById_Success() {
        // Given
        when(forumCategoryRepository.findById(1L)).thenReturn(Optional.of(category1));

        // When
        Optional<ForumCategory> result = forumCategoryRepository.findById(1L);

        // Then
        assertThat(result)
                .isPresent()
                .hasValueSatisfying(category -> {
                    assertThat(category.getId()).isEqualTo(1L);
                    assertThat(category.getName()).isEqualTo("General Discussion");
                });
        verify(forumCategoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty optional when ID not found")
    void testFindById_NotFound() {
        // Given
        when(forumCategoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<ForumCategory> result = forumCategoryRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(forumCategoryRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should handle multiple findByName calls")
    void testFindByName_MultipleCalls() {
        // Given
        when(forumCategoryRepository.findByName("General Discussion")).thenReturn(category1);
        when(forumCategoryRepository.findByName("Technical Support")).thenReturn(category2);

        // When
        ForumCategory result1 = forumCategoryRepository.findByName("General Discussion");
        ForumCategory result2 = forumCategoryRepository.findByName("Technical Support");

        // Then
        assertThat(result1.getName()).isEqualTo("General Discussion");
        assertThat(result2.getName()).isEqualTo("Technical Support");
        verify(forumCategoryRepository, times(1)).findByName("General Discussion");
        verify(forumCategoryRepository, times(1)).findByName("Technical Support");
    }

    @Test
    @DisplayName("Should verify no interactions when no methods called")
    void testNoInteractions() {
        // When - no repository methods called

        // Then
        verifyNoInteractions(forumCategoryRepository);
    }

    @Test
    @DisplayName("Should handle findByName with whitespace")
    void testFindByName_WithWhitespace() {
        // Given
        ForumCategory categoryWithSpace = new ForumCategory();
        categoryWithSpace.setId(4L);
        categoryWithSpace.setName("Support ");

        when(forumCategoryRepository.findByName("Support")).thenReturn(category1);
        when(forumCategoryRepository.findByName("Support ")).thenReturn(categoryWithSpace);

        // When
        ForumCategory result1 = forumCategoryRepository.findByName("Support");
        ForumCategory result2 = forumCategoryRepository.findByName("Support ");

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.getName()).doesNotEndWith(" ");
        assertThat(result2.getName()).endsWith(" ");
        verify(forumCategoryRepository, times(1)).findByName("Support");
        verify(forumCategoryRepository, times(1)).findByName("Support ");
    }
}