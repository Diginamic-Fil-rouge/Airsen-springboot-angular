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
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ForumThreadRepositoryIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    // -----------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------

    private User createUser(String username, String email) {
        User u = new User();
        u.setFirstName(username);
        u.setLastName("Doe");
        u.setEmail(email);
        u.setPassword("password");
        u.setRole(UserRole.USER);
        u.setCreatedAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    private ForumCategory createCategory(String name) {
        ForumCategory cat = new ForumCategory();
        cat.setName(name);
        cat.setColor("#FF0000");
        cat.setDescription("Description");
        return categoryRepository.save(cat);
    }

    private ForumThread createThread(String title, String content, User author, ForumCategory category) {
        ForumThread thread = new ForumThread();
        thread.setTitle(title);
        thread.setContent(content);
        thread.setAuthor(author);
        thread.setCategory(category);
        thread.setCreatedDate(LocalDateTime.now());
        thread.setMessages(new ArrayList<>());
        return threadRepository.save(thread);
    }

    // -----------------------------------------------------------
    // Tests
    // -----------------------------------------------------------

    @Test
    @DisplayName("Should save and retrieve all threads")
    void testFindAll() {
        User user = createUser("john", "john@example.com");
        ForumCategory cat = createCategory("General");

        createThread("Thread 1", "Content A", user, cat);
        createThread("Thread 2", "Content B", user, cat);

        List<ForumThread> threads = threadRepository.findAll();

        assertThat(threads)
                .hasSize(2)
                .extracting(ForumThread::getTitle)
                .containsExactlyInAnyOrder("Thread 1", "Thread 2");
    }

    @Test
    @DisplayName("Should load related data eagerly with findAllWithRelations")
    void testFindAllWithRelations() {
        User author = createUser("alice", "alice@example.com");
        ForumCategory cat = createCategory("Tech");

        createThread("Eager Thread", "Hello", author, cat);

        List<ForumThread> threads = threadRepository.findAllWithRelations();

        assertThat(threads).hasSize(1);
        assertThat(threads.get(0).getAuthor()).isNotNull();
        assertThat(threads.get(0).getCategory()).isNotNull();
    }

    @Test
    @DisplayName("Should find threads by category")
    void testFindByCategory() {
        User user = createUser("tom", "tom@example.com");
        ForumCategory news = createCategory("News");
        ForumCategory sports = createCategory("Sports");

        createThread("News Thread", "Breaking", user, news);
        createThread("Sports Thread", "Team wins", user, sports);

        List<ForumThread> result = threadRepository.findByCategory(news);

        assertThat(result)
                .hasSize(1)
                .extracting(ForumThread::getTitle)
                .containsExactly("News Thread");
    }

    @Test
    @DisplayName("Should find threads by author")
    void testFindByAuthor() {
        User author1 = createUser("bob", "bob@example.com");
        User author2 = createUser("will", "will@example.com");
        ForumCategory cat = createCategory("Chat");

        createThread("Bob's Thread", "A", author1, cat);
        createThread("Will's Thread", "B", author2, cat);

        List<ForumThread> result = threadRepository.findByAuthor(author1);

        assertThat(result)
                .hasSize(1)
                .extracting(ForumThread::getTitle)
                .containsExactly("Bob's Thread");
    }

    @Test
    @DisplayName("Should load messages eagerly with findByIdWithMessages")
    void testFindByIdWithMessages() {
        User user = createUser("charles", "charles@example.com");
        ForumCategory cat = createCategory("Gaming");

        ForumThread thread = createThread("Game Thread", "Content", user, cat);

        // Add a message manually
        ForumMessage msg = new ForumMessage();
        msg.setContent("Welcome");
        msg.setThread(thread);
        msg.setAuthor(user);
        msg.setCreatedDate(LocalDateTime.now());
        thread.getMessages().add(msg);

        thread = threadRepository.save(thread);

        Optional<ForumThread> found = threadRepository.findByIdWithMessages(thread.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMessages()).hasSize(1);
        assertThat(found.get().getAuthor()).isNotNull();
        assertThat(found.get().getCategory()).isNotNull();
    }

    @Test
    @DisplayName("Should load threads by category with messages eagerly")
    void testFindByCategoryWithMessages() {
        User user = createUser("dave", "dave@example.com");
        ForumCategory cat = createCategory("Movies");

        ForumThread thread = createThread("Best Movies", "Discuss", user, cat);

        ForumMessage msg = new ForumMessage();
        msg.setContent("Hello");
        msg.setThread(thread);
        msg.setAuthor(user);
        msg.setCreatedDate(LocalDateTime.now());
        thread.getMessages().add(msg);

        threadRepository.save(thread);

        List<ForumThread> result = threadRepository.findByCategoryWithMessages(cat);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessages()).hasSize(1);
    }

    @Test
    @DisplayName("Should delete a thread by ID")
    void testDeleteById() {
        User user = createUser("eve", "eve@example.com");
        ForumCategory cat = createCategory("Random");

        ForumThread thread = createThread("Delete me", "Content", user, cat);

        Long id = thread.getId();
        threadRepository.deleteById(id);

        assertThat(threadRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Should count threads by author ID")
    void testCountByAuthor() {
        User user = createUser("zoe", "zoe@example.com");
        ForumCategory cat = createCategory("Stuff");

        createThread("Thread A", "A", user, cat);
        createThread("Thread B", "B", user, cat);

        long count = threadRepository.countByAuthorId(user.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should paginate threads by category ID")
    void testFindByCategoryIdWithPagination() {
        User user = createUser("paul", "paul@example.com");
        ForumCategory cat = createCategory("Cars");

        createThread("Car A", "A", user, cat);
        createThread("Car B", "B", user, cat);
        createThread("Car C", "C", user, cat);

        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdDate").descending());

        Page<ForumThread> page = threadRepository.findByCategoryId(cat.getId(), pageable);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should search threads by title or content (pagination)")
    void testSearchByTitleOrContent() {
        User user = createUser("sam", "sam@example.com");
        ForumCategory cat = createCategory("Books");

        createThread("Harry Potter", "Wizard stuff", user, cat);
        createThread("Cooking Basics", "Kitchen tips", user, cat);

        Pageable pageable = PageRequest.of(0, 10);

        Page<ForumThread> result = threadRepository
                .findByTitleContainingOrContentContaining("wizard", "wizard", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Harry Potter");
    }

    @Test
    @DisplayName("Should search by category ID + title/content")
    void testSearchByCategoryIdAndTitleOrContent() {
        User user = createUser("logan", "logan@example.com");
        ForumCategory programming = createCategory("Programming");
        ForumCategory cooking = createCategory("Cooking");

        createThread("Java Tips", "Streams and Lambdas", user, programming);
        createThread("Kitchen Tips", "Recipes", user, cooking);

        Pageable pageable = PageRequest.of(0, 10);

        Page<ForumThread> result = threadRepository
                .findByCategoryIdAndTitleContainingOrContentContaining(
                        programming.getId(),
                        "tips",
                        "tips",
                        pageable
                );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java Tips");
    }
}
