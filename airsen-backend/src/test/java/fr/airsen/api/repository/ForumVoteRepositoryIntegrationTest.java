package fr.airsen.api.repository;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.entity.enums.VoteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ForumVoteRepositoryIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private ForumVoteRepository voteRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private User createUser(String username) {
        User u = new User();
        u.setFirstName(username);
        u.setLastName("Doe");
        u.setEmail(username + "@example.com");
        u.setPassword("password");
        u.setRole(UserRole.USER);
        u.setCreatedAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    private ForumCategory createCategory(String name) {
        ForumCategory c = new ForumCategory();
        c.setName(name);
        c.setColor("#FF0000");
        c.setDescription("Description");
        return categoryRepository.save(c);
    }

    private ForumThread createThread(String title, String content, User author, ForumCategory category) {
        ForumThread t = new ForumThread();
        t.setTitle(title);
        t.setContent(content);
        t.setAuthor(author);
        t.setCategory(category);
        t.setCreatedDate(LocalDateTime.now());
        t.setMessages(new ArrayList<>());
        return threadRepository.save(t);
    }

    private ForumVote createVote(User user, ForumThread thread, boolean upvote) {
        ForumVote v = new ForumVote();
        v.setUser(user);
        v.setThread(thread);
        v.setVoteType(upvote ? VoteType.LIKE : VoteType.DISLIKE);
        return voteRepository.save(v);
    }

    // ---------------------------------------------------------
    // Tests
    // ---------------------------------------------------------

    @Test
    @DisplayName("Should save and retrieve all votes")
    void testFindAll() {
        User user = createUser("john");
        User user2 = createUser("Alice");
        ForumCategory cat = createCategory("General");
        ForumThread thread = createThread("Hello", "World", user, cat);

        createVote(user, thread, true);
        createVote(user2, thread, false);

        List<ForumVote> votes = voteRepository.findAll();

        assertThat(votes).hasSize(2);
    }

    @Test
    @DisplayName("Should find votes by thread")
    void testFindByThread() {
        User user1 = createUser("alice");
        User user2 = createUser("bob");

        ForumCategory cat = createCategory("Tech");
        ForumThread thread1 = createThread("Tech Thread", "AAA", user1, cat);
        ForumThread thread2 = createThread("Other Thread", "BBB", user1, cat);

        createVote(user1, thread1, true);
        createVote(user2, thread1, false);
        createVote(user2, thread2, true);

        List<ForumVote> result = voteRepository.findByThread(thread1);

        assertThat(result)
                .hasSize(2)
                .extracting(v -> v.getUser().getFirstName())
                .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    @DisplayName("Should find vote by user and thread")
    void testFindByUserAndThread() {
        User alice = createUser("alice");
        User bob = createUser("bob");

        ForumCategory cat = createCategory("Movies");
        ForumThread thread = createThread("Best Movies", "Discuss", alice, cat);

        createVote(alice, thread, true);
        createVote(bob, thread, false);

        ForumVote result = voteRepository.findByUserAndThread(bob, thread);

        assertThat(result).isNotNull();
        assertThat(result.getVoteType()).isEqualTo(VoteType.DISLIKE);
        assertThat(result.getUser().getFirstName()).isEqualTo("bob");
    }

    @Test
    @DisplayName("Should return null if user has not voted on thread")
    void testFindByUserAndThreadNotFound() {
        User user = createUser("charles");
        ForumCategory cat = createCategory("Gaming");
        ForumThread thread = createThread("Game Thread", "Content", user, cat);

        ForumVote result = voteRepository.findByUserAndThread(user, thread);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should delete vote by ID")
    void testDeleteById() {
        User user = createUser("eve");
        ForumCategory cat = createCategory("Random");
        ForumThread thread = createThread("Random Thread", "Content", user, cat);

        ForumVote vote = createVote(user, thread, true);

        Long id = vote.getId();
        voteRepository.deleteById(id);

        assertThat(voteRepository.findById(id)).isEmpty();
    }
}
