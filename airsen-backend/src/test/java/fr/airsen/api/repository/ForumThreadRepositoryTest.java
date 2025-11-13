package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ForumThreadRepository (mocked repository behavior).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ForumThreadRepository Tests")
class ForumThreadRepositoryTest {

    @Mock
    private ForumThreadRepository repository;

    private ForumCategory category;
    private User author;
    private ForumThread thread1;
    private ForumThread thread2;

    @BeforeEach
    void setUp() {
        category = new ForumCategory();
        category.setId(10L);
        category.setName("General Discussion");

        author = new User();
        author.setId(1L);
        author.setFirstName("alice");
        author.setLastName("vanneneker");

        thread1 = new ForumThread();
        thread1.setId(100L);
        thread1.setTitle("Hello World");
        thread1.setContent("This is the first thread.");
        thread1.setAuthor(author);
        thread1.setCategory(category);
        thread1.setCreatedDate(LocalDateTime.now());

        thread2 = new ForumThread();
        thread2.setId(101L);
        thread2.setTitle("Second Thread");
        thread2.setContent("This is another discussion thread.");
        thread2.setAuthor(author);
        thread2.setCategory(category);
        thread2.setCreatedDate(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testFindAll() {
        when(repository.findAll()).thenReturn(List.of(thread1, thread2));

        List<ForumThread> result = repository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Hello World");
        verify(repository).findAll();
    }

    @Test
    void testFindAllWithRelations() {
        when(repository.findAllWithRelations()).thenReturn(List.of(thread1, thread2));

        List<ForumThread> result = repository.findAllWithRelations();

        assertThat(result).containsExactly(thread1, thread2);
        verify(repository).findAllWithRelations();
    }

    @Test
    void testFindByCategory() {
        when(repository.findByCategory(category)).thenReturn(List.of(thread1, thread2));

        List<ForumThread> result = repository.findByCategory(category);

        assertThat(result).allMatch(t -> t.getCategory().equals(category));
        verify(repository).findByCategory(category);
    }

    @Test
    void testFindByAuthor() {
        when(repository.findByAuthor(author)).thenReturn(List.of(thread1, thread2));

        List<ForumThread> result = repository.findByAuthor(author);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAuthor()).isEqualTo(author);
        verify(repository).findByAuthor(author);
    }

    @Test
    void testFindByIdWithMessages() {
        when(repository.findByIdWithMessages(100L)).thenReturn(Optional.of(thread1));

        Optional<ForumThread> result = repository.findByIdWithMessages(100L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(100L);
        verify(repository).findByIdWithMessages(100L);
    }

    @Test
    void testFindByCategoryWithMessages() {
        when(repository.findByCategoryWithMessages(category)).thenReturn(List.of(thread1, thread2));

        List<ForumThread> result = repository.findByCategoryWithMessages(category);

        assertThat(result).hasSize(2);
        verify(repository).findByCategoryWithMessages(category);
    }

    @Test
    void testDeleteById() {
        doNothing().when(repository).deleteById(100L);

        repository.deleteById(100L);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(repository).deleteById(captor.capture());
        assertThat(captor.getValue()).isEqualTo(100L);
    }

    @Test
    void testCountByAuthorId() {
        when(repository.countByAuthorId(1L)).thenReturn(5L);

        long count = repository.countByAuthorId(1L);

        assertThat(count).isEqualTo(5L);
        verify(repository).countByAuthorId(1L);
    }

    @Test
    void testFindByCategoryIdWithPagination() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ForumThread> page = new PageImpl<>(List.of(thread1, thread2), pageable, 2);
        when(repository.findByCategoryId(10L, pageable)).thenReturn(page);

        Page<ForumThread> result = repository.findByCategoryId(10L, pageable);

        assertThat(result.getContent()).containsExactly(thread1, thread2);
        verify(repository).findByCategoryId(10L, pageable);
    }

    @Test
    void testFindByTitleContainingOrContentContaining() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<ForumThread> expectedPage = new PageImpl<>(List.of(thread1), pageable, 1);
        when(repository.findByTitleContainingOrContentContaining("hello", "hello", pageable))
                .thenReturn(expectedPage);

        Page<ForumThread> result = repository.findByTitleContainingOrContentContaining("hello", "hello", pageable);

        assertThat(result.getContent()).containsExactly(thread1);
        verify(repository).findByTitleContainingOrContentContaining("hello", "hello", pageable);
    }

    @Test
    void testFindByCategoryIdAndTitleContainingOrContentContaining() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<ForumThread> expectedPage = new PageImpl<>(List.of(thread2), pageable, 1);
        when(repository.findByCategoryIdAndTitleContainingOrContentContaining(10L, "second", "second", pageable))
                .thenReturn(expectedPage);

        Page<ForumThread> result = repository.findByCategoryIdAndTitleContainingOrContentContaining(
                10L, "second", "second", pageable);

        assertThat(result.getContent()).containsExactly(thread2);
        verify(repository).findByCategoryIdAndTitleContainingOrContentContaining(10L, "second", "second", pageable);
    }

    @Test
    void testFindByCategoryWithNoResults() {
        when(repository.findByCategory(any())).thenReturn(Collections.emptyList());

        List<ForumThread> result = repository.findByCategory(new ForumCategory());

        assertThat(result).isEmpty();
        verify(repository).findByCategory(any());
    }
}
