package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.VoteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ForumVoteRepositoryTest {

    @Autowired
    private ForumVoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Test
    @DisplayName("Test saving and finding ForumVote")
    void testSaveAndFind() {
        User user = new User();
        user.setFirstName("alice");
        user.setLastName("vanneneker");
        userRepository.save(user);

        ForumThread thread = new ForumThread();
        thread.setTitle("Test Thread");
        threadRepository.save(thread);

        ForumVote vote = new ForumVote();
        vote.setUser(user);
        vote.setThread(thread);
        vote.setVoteType(VoteType.LIKE);

        ForumVote savedVote = voteRepository.save(vote);
        assertThat(savedVote.getId()).isNotNull();

        // findAll
        List<ForumVote> allVotes = voteRepository.findAll();
        assertThat(allVotes).hasSize(1).contains(savedVote);

        // findByThread
        List<ForumVote> votesByThread = voteRepository.findByThread(thread);
        assertThat(votesByThread).hasSize(1).contains(savedVote);

        // findByUserAndThread
        ForumVote foundVote = voteRepository.findByUserAndThread(user, thread);
        assertThat(foundVote).isNotNull();
        assertThat(foundVote.getUser()).isEqualTo(user);
        assertThat(foundVote.getThread()).isEqualTo(thread);
        assertThat(foundVote.getVoteType()).isEqualTo(VoteType.LIKE);
    }

    @Test
    @DisplayName("Test findByUserAndThread returns null if no vote exists")
    void testFindByUserAndThreadNotFound() {
        User user = new User();
        user.setFirstName("bob");
        user.setLastName("vanneneker");
        userRepository.save(user);

        ForumThread thread = new ForumThread();
        thread.setTitle("Thread 2");
        threadRepository.save(thread);

        ForumVote vote = voteRepository.findByUserAndThread(user, thread);
        assertThat(vote).isNull();
    }
}
