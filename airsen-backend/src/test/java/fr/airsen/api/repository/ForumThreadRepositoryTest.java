package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.ForumMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ForumThreadRepositoryTest {

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private ForumMessageRepository messageRepository;

    @Test
    @DisplayName("Test save and basic find operations")
    void testSaveAndFind() {
        User author = new User();
        author.setFirstName("alice");
        author.setLastName("vanneneker");

        userRepository.save(author);

        ForumCategory category = new ForumCategory();
        category.setName("General");
        categoryRepository.save(category);

        ForumThread thread = new ForumThread();
        thread.setTitle("Test Thread");
        thread.setContent("Thread content");
        thread.setAuthor(author);
        thread.setCategory(category);

        ForumThread savedThread = threadRepository.save(thread);

        assertThat(savedThread.getId()).isNotNull();

        // Basic findAll
        List<ForumThread> allThreads = threadRepository.findAll();
        assertThat(allThreads).hasSize(1).contains(savedThread);

        // Find by author
        List<ForumThread> byAuthor = threadRepository.findByAuthor(author);
        assertThat(byAuthor).hasSize(1).contains(savedThread);

        // Find by category
        List<ForumThread> byCategory = threadRepository.findByCategory(category);
        assertThat(byCategory).hasSize(1).contains(savedThread);

        // Count by author
        long count = threadRepository.countByAuthorId(author.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Test deleteById")
    void testDeleteById() {
        User author = new User();
        author.setFirstName("bob");
        author.setLastName("vanneneker");

        userRepository.save(author);

        ForumCategory category = new ForumCategory();
        category.setName("Tech");
        categoryRepository.save(category);

        ForumThread thread = new ForumThread();
        thread.setTitle("Delete Thread");
        thread.setAuthor(author);
        thread.setCategory(category);

        ForumThread savedThread = threadRepository.save(thread);
        Long threadId = savedThread.getId();

        assertThat(threadRepository.findById(threadId)).isPresent();

        threadRepository.deleteById(threadId);

        Optional<ForumThread> deleted = threadRepository.findById(threadId);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Test findAllWithRelations and findByIdWithMessages")
    void testCustomQueries() {
        User author = new User();
        author.setFirstName("charlie");
        author.setLastName("vanneneker");

        userRepository.save(author);

        ForumCategory category = new ForumCategory();
        category.setName("News");
        categoryRepository.save(category);

        ForumThread thread = new ForumThread();
        thread.setTitle("Thread With Messages");
        thread.setAuthor(author);
        thread.setCategory(category);
        threadRepository.save(thread);

        ForumMessage message = new ForumMessage();
        message.setContent("First message");
        message.setAuthor(author);
        message.setThread(thread);
        messageRepository.save(message);

        // Test findAllWithRelations
        List<ForumThread> threadsWithRelations = threadRepository.findAllWithRelations();
        assertThat(threadsWithRelations).hasSize(1);
        assertThat(threadsWithRelations.get(0).getAuthor()).isNotNull();
        assertThat(threadsWithRelations.get(0).getCategory()).isNotNull();

        // Test findByIdWithMessages
        Optional<ForumThread> threadWithMessages = threadRepository.findByIdWithMessages(thread.getId());
        assertThat(threadWithMessages).isPresent();
        assertThat(threadWithMessages.get().getMessages()).hasSize(1);
    }

    @Test
    @DisplayName("Test pagination and search queries")
    void testPaginationAndSearch() {
        User author = new User();
        author.setFirstName("dave");
        author.setLastName("vanneneker");

        userRepository.save(author);

        ForumCategory category = new ForumCategory();
        category.setName("Science");
        categoryRepository.save(category);

        // Save multiple threads
        for (int i = 1; i <= 5; i++) {
            ForumThread thread = new ForumThread();
            thread.setTitle("Science Thread " + i);
            thread.setContent("Content " + i);
            thread.setAuthor(author);
            thread.setCategory(category);
            threadRepository.save(thread);
        }

        PageRequest pageable = PageRequest.of(0, 3);

        // Find by category with pagination
        Page<ForumThread> page = threadRepository.findByCategoryId(category.getId(), pageable);
        assertThat(page.getContent()).hasSize(3);

        // Search by title or content
        Page<ForumThread> searchPage = threadRepository.findByTitleContainingOrContentContaining("Thread 1", "Thread 1", pageable);
        assertThat(searchPage.getContent()).hasSize(1);

        // Search by category and term
        Page<ForumThread> searchByCategory = threadRepository.findByCategoryIdAndTitleContainingOrContentContaining(category.getId(), "Thread 2", "Thread 2", pageable);
        assertThat(searchByCategory.getContent()).hasSize(1);
    }
}
