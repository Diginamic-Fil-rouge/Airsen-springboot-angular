package fr.airsen.api.dto;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.mapper.ForumMessageMapper;
import fr.airsen.api.repository.ForumCategoryRepository;
import fr.airsen.api.repository.ForumMessageRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ForumMessageDTOIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private ForumMessageRepository messageRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumMessageMapper mapper;

    @Autowired
    EntityManager entityManager;

    // ---------------------------
    // Helper methods
    // ---------------------------

    private User createUser(String username) {
        User u = new User();
        u.setFirstName(username);
        u.setEmail(username + "@example.com");
        u.setRole(UserRole.USER);
        u.setPassword("password");
        u.setCreatedAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    private ForumCategory createCategory(String name) {
        ForumCategory cat = new ForumCategory();
        cat.setName(name);
        cat.setDescription("Desc " + name);
        cat.setColor("#123456");
        cat.setThreads(new ArrayList<>());
        return categoryRepository.save(cat);
    }

    private ForumThread createThread(String title, User author, ForumCategory category) {
        ForumThread t = new ForumThread();
        t.setTitle(title);
        t.setContent(title + " content");
        t.setCreatedDate(LocalDateTime.now());
        t.setLastMessageDate(LocalDateTime.now());
        t.setAuthor(author);
        t.setCategory(category);
        return threadRepository.save(t);
    }

    private ForumMessage createMessage(String content, User author, ForumThread thread) {
        ForumMessage m = new ForumMessage();
        m.setContent(content);
        m.setAuthor(author);
        m.setThread(thread);
        m.setCreatedDate(LocalDateTime.now());
        return messageRepository.save(m);
    }

    // ---------------------------
    // Tests
    // ---------------------------

    @Test
    @DisplayName("DTO constructor maps entity to DTO correctly")
    void testDTOConstructorWithThread() {

        User user = createUser("alice");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Test Thread", user, category);
        ForumMessage message = createMessage("Hello world", user, thread);

        entityManager.flush();
        entityManager.clear();

        ForumMessage reloaded = messageRepository.findById(message.getId()).orElseThrow();

        ForumMessageDTO dto = new ForumMessageDTO(reloaded, true);

        assertThat(dto.getId()).isEqualTo(message.getId());
        assertThat(dto.getContent()).isEqualTo("Hello world");
        assertThat(dto.getCreatedDate().truncatedTo(ChronoUnit.MICROS)).isEqualTo(message.getCreatedDate().truncatedTo(ChronoUnit.MICROS));

        // Nested author DTO
        assertThat(dto.getAuthor()).isNotNull();
        assertThat(dto.getAuthor().getFirstName()).isEqualTo("alice");

        // Nested thread DTO
        assertThat(dto.getThread()).isNotNull();
        assertThat(dto.getThread().getId()).isEqualTo(thread.getId());
        assertThat(dto.getThread().getTitle()).isEqualTo("Test Thread");
    }

    @Test
    @DisplayName("DTO constructor excludes thread when withThread=false")
    void testDTOConstructorWithoutThread() {

        User user = createUser("bob");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread 2", user, category);
        ForumMessage message = createMessage("Message content", user, thread);

        entityManager.flush();
        entityManager.clear();

        ForumMessage reloaded = messageRepository.findById(message.getId()).orElseThrow();

        ForumMessageDTO dto = new ForumMessageDTO(reloaded, false);

        assertThat(dto.getThread()).isNull();
        assertThat(dto.getAuthor()).isNotNull();
        assertThat(dto.getAuthor().getFirstName()).isEqualTo("bob");
    }

    @Test
    @DisplayName("DTO → Entity mapping works and persists in database")
    void testDTOToEntityPersistence() {

        User user = createUser("charlie");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread 3", user, category);

        ForumMessageDTO dto = new ForumMessageDTO();
        dto.setContent("DTO message");
        dto.setCreatedDate(LocalDateTime.now());

        // Set author DTO
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        dto.setAuthor(userDTO);

        // Map to entity
        ForumMessage entity = mapper.toEntity(dto);
        entity.setThread(thread); // manually set thread for persistence

        ForumMessage saved = messageRepository.save(entity);

        entityManager.flush();
        entityManager.clear();

        ForumMessage loaded = messageRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getContent()).isEqualTo("DTO message");
        assertThat(loaded.getAuthor()).isNotNull();
        assertThat(loaded.getAuthor().getId()).isEqualTo(user.getId());
        assertThat(loaded.getThread().getId()).isEqualTo(thread.getId());
    }

    @Test
    @DisplayName("Entity → DTO → Entity round-trip preserves fields")
    void testRoundTripConversion() {

        User user = createUser("dave");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread 4", user, category);
        ForumMessage message = createMessage("Round trip test", user, thread);

        ForumMessageDTO dto = mapper.toDTO(message);
        ForumMessage roundTrip = mapper.toEntity(dto);
        roundTrip.setThread(thread); // thread must be re-set

        assertThat(roundTrip.getContent()).isEqualTo(message.getContent());
        assertThat(roundTrip.getAuthor().getId()).isEqualTo(message.getAuthor().getId());
    }

    @Test
    @DisplayName("List mapping works correctly")
    void testListMapping() {

        User user = createUser("eve");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread 5", user, category);

        createMessage("Msg 1", user, thread);
        createMessage("Msg 2", user, thread);

        entityManager.flush();
        entityManager.clear();

        List<ForumMessage> all = messageRepository.findAll();
        List<ForumMessageDTO> dtoList = mapper.toDTOs(all);

        assertThat(dtoList).hasSize(2);
        assertThat(dtoList)
                .extracting(ForumMessageDTO::getContent)
                .containsExactlyInAnyOrder("Msg 1", "Msg 2");
    }
}
