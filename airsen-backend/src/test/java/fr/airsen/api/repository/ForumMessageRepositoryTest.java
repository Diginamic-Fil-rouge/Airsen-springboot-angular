package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ForumMessageRepository (mocked behavior).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ForumMessageRepository Tests")
class ForumMessageRepositoryTest {

    @Mock
    private ForumMessageRepository repository;

    private ForumThread thread;
    private User author;
    private ForumMessage message1;
    private ForumMessage message2;

    @BeforeEach
    void setUp() {
        thread = new ForumThread();
        thread.setId(1L);
        thread.setTitle("Test Thread");

        author = new User();
        author.setId(1L);
        author.setFirstName("john");
        author.setLastName("doe");

        message1 = new ForumMessage();
        message1.setId(100L);
        message1.setAuthor(author);
        message1.setThread(thread);
        message1.setContent("First message");

        message2 = new ForumMessage();
        message2.setId(101L);
        message2.setAuthor(author);
        message2.setThread(thread);
        message2.setContent("Second message");
    }

    @Test
    void testFindAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(message1, message2));

        List<ForumMessage> result = repository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(message1, message2);
        verify(repository, times(1)).findAll();
    }

    @Test
    void testFindByThread() {
        when(repository.findByThread(thread)).thenReturn(List.of(message1, message2));

        List<ForumMessage> result = repository.findByThread(thread);

        assertThat(result).containsExactly(message1, message2);
        verify(repository).findByThread(thread);
    }

    @Test
    void testFindByAuthor() {
        when(repository.findByAuthor(author)).thenReturn(List.of(message1, message2));

        List<ForumMessage> result = repository.findByAuthor(author);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAuthor()).isEqualTo(author);
        verify(repository).findByAuthor(author);
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
        when(repository.countByAuthorId(1L)).thenReturn(2L);

        long count = repository.countByAuthorId(1L);

        assertThat(count).isEqualTo(2L);
        verify(repository).countByAuthorId(1L);
    }

    @Test
    void testFindByThread_NoResults() {
        when(repository.findByThread(any())).thenReturn(Collections.emptyList());

        List<ForumMessage> result = repository.findByThread(new ForumThread());

        assertThat(result).isEmpty();
        verify(repository).findByThread(any());
    }
}
