package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ForumMessageRepositoryTest {

    @Autowired
    private ForumMessageRepository messageRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Test saving and retrieving ForumMessage")
    void testSaveAndFind() {
        User author = new User();
        author.setFirstName("alice");
        author.setLastName("vanneneker");

        userRepository.save(author);

        ForumThread thread = new ForumThread();
        thread.setTitle("Test Thread");
        threadRepository.save(thread);

        ForumMessage message = new ForumMessage();
        message.setContent("Hello World");
        message.setAuthor(author);
        message.setThread(thread);

        ForumMessage savedMessage = messageRepository.save(message);

        assertThat(savedMessage.getId()).isNotNull();

        List<ForumMessage> allMessages = messageRepository.findAll();
        assertThat(allMessages).hasSize(1).contains(savedMessage);

        List<ForumMessage> messagesByThread = messageRepository.findByThread(thread);
        assertThat(messagesByThread).hasSize(1).contains(savedMessage);

        List<ForumMessage> messagesByAuthor = messageRepository.findByAuthor(author);
        assertThat(messagesByAuthor).hasSize(1).contains(savedMessage);

        long count = messageRepository.countByAuthorId(author.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Test deleteById")
    void testDeleteById() {
        User author = new User();
        author.setFirstName("bob");
        author.setLastName("vanneneker");

        userRepository.save(author);

        ForumThread thread = new ForumThread();
        thread.setTitle("Thread to Delete");
        threadRepository.save(thread);

        ForumMessage message = new ForumMessage();
        message.setContent("Delete me");
        message.setAuthor(author);
        message.setThread(thread);

        ForumMessage savedMessage = messageRepository.save(message);
        Long messageId = savedMessage.getId();

        assertThat(messageRepository.findById(messageId)).isPresent();

        messageRepository.deleteById(messageId);

        Optional<ForumMessage> deleted = messageRepository.findById(messageId);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Test countByAuthorId returns 0 if author has no messages")
    void testCountByAuthorIdNotFound() {
        User author = new User();
        author.setFirstName("charlie");
        author.setLastName("vanneneker");

        userRepository.save(author);

        long count = messageRepository.countByAuthorId(author.getId());
        assertThat(count).isZero();
    }
}
