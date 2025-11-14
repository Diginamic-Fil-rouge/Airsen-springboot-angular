package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ForumVoteRepository (mocked repository behavior).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ForumVoteRepository Tests")
class ForumVoteRepositoryTest {

    @Mock
    private ForumVoteRepository repository;

    private User user1;
    private User user2;
    private ForumThread thread1;
    private ForumVote vote1;
    private ForumVote vote2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setFirstName("alice");
        user1.setLastName("vanneneker");

        user2 = new User();
        user2.setId(2L);
        user2.setFirstName("bob");
        user2.setLastName("vanneneker");

        thread1 = new ForumThread();
        thread1.setId(10L);
        thread1.setTitle("Best practices in Java");

        vote1 = new ForumVote();
        vote1.setId(100L);
        vote1.setUser(user1);
        vote1.setThread(thread1);
        vote1.setVoteType(VoteType.LIKE);

        vote2 = new ForumVote();
        vote2.setId(101L);
        vote2.setUser(user2);
        vote2.setThread(thread1);
        vote2.setVoteType(VoteType.DISLIKE);
    }

    @Test
    void testFindAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(vote1, vote2));

        List<ForumVote> result = repository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUser()).isEqualTo(user1);
        assertThat(result.get(1).getUser()).isEqualTo(user2);
        verify(repository, times(1)).findAll();
    }

    @Test
    void testFindByThread() {
        when(repository.findByThread(thread1)).thenReturn(List.of(vote1, vote2));

        List<ForumVote> result = repository.findByThread(thread1);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(v -> v.getThread().equals(thread1));
        verify(repository, times(1)).findByThread(thread1);
    }

    @Test
    void testFindByUserAndThread() {
        when(repository.findByUserAndThread(user1, thread1)).thenReturn(vote1);

        ForumVote result = repository.findByUserAndThread(user1, thread1);

        assertThat(result).isEqualTo(vote1);
        assertThat(result.getVoteType()).isEqualTo(VoteType.LIKE);
        verify(repository).findByUserAndThread(user1, thread1);
    }

    @Test
    void testFindByThread_NoResults() {
        when(repository.findByThread(any())).thenReturn(Collections.emptyList());

        List<ForumVote> result = repository.findByThread(new ForumThread());

        assertThat(result).isEmpty();
        verify(repository).findByThread(any());
    }

    @Test
    void testFindByUserAndThread_NoResult() {
        when(repository.findByUserAndThread(any(), any())).thenReturn(null);

        ForumVote result = repository.findByUserAndThread(new User(), new ForumThread());

        assertThat(result).isNull();
        verify(repository).findByUserAndThread(any(), any());
    }
}
