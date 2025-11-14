package fr.airsen.api.repository;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ForumMessageRepositoryIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private ForumMessageRepository messageRepository;

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

    private ForumMessage createMessage(String content, User author, ForumThread thread) {
        ForumMessage msg = new ForumMessage();
        msg.setContent(content);
        msg.setAuthor(author);
        msg.setThread(thread);
        msg.setCreatedDate(LocalDateTime.now());
        return messageRepository.save(msg);
    }

    // ---------------------------------------------------------
    // Tests
    // ---------------------------------------------------------

    @Test
    @DisplayName("Should save and retrieve all messages")
    void testFindAll() {
        User user = createUser("john");
        ForumCategory category = createCategory("General");
        ForumThread thread = createThread("Hello", "World", user, category);

        createMessage("Message 1", user, thread);
        createMessage("Message 2", user, thread);

        List<ForumMessage> messages = messageRepository.findAll();

        assertThat(messages)
                .hasSize(2)
                .extracting(ForumMessage::getContent)
                .containsExactlyInAnyOrder("Message 1", "Message 2");
    }

    @Test
    @DisplayName("Should find messages by thread")
    void testFindByThread() {
        User user = createUser("anna");
        ForumCategory cat = createCategory("Tech");

        ForumThread thread1 = createThread("Thread 1", "AAAA", user, cat);
        ForumThread thread2 = createThread("Thread 2", "BBBB", user, cat);

        createMessage("Msg A", user, thread1);
        createMessage("Msg B", user, thread2);

        List<ForumMessage> result = messageRepository.findByThread(thread1);

        assertThat(result)
                .hasSize(1)
                .extracting(ForumMessage::getContent)
                .containsExactly("Msg A");
    }

    @Test
    @DisplayName("Should find messages by author")
    void testFindByAuthor() {
        User author1 = createUser("marc");
        User author2 = createUser("peter");

        ForumCategory cat = createCategory("Random");
        ForumThread thread = createThread("Topic", "Content", author1, cat);

        createMessage("Msg by Marc", author1, thread);
        createMessage("Msg by Peter", author2, thread);

        List<ForumMessage> result = messageRepository.findByAuthor(author1);

        assertThat(result)
                .hasSize(1)
                .extracting(ForumMessage::getContent)
                .containsExactly("Msg by Marc");
    }

    @Test
    @DisplayName("Should delete a message by ID")
    void testDeleteById() {
        User user = createUser("will");
        ForumCategory cat = createCategory("News");
        ForumThread thread = createThread("Thread", "Content", user, cat);

        ForumMessage message = createMessage("Will delete", user, thread);

        Long id = message.getId();
        messageRepository.deleteById(id);

        assertThat(messageRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Should count messages by author ID")
    void testCountByAuthorId() {
        User author = createUser("zoe");
        User other = createUser("bob");

        ForumCategory cat = createCategory("Movies");
        ForumThread thread = createThread("Movies Thread", "Content", author, cat);

        createMessage("Zoe 1", author, thread);
        createMessage("Zoe 2", author, thread);
        createMessage("Bob msg", other, thread);

        long count = messageRepository.countByAuthorId(author.getId());

        assertThat(count).isEqualTo(2);
    }
}
