package fr.airsen.api.dto;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.*;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.entity.enums.VoteType;
import fr.airsen.api.mapper.ForumThreadMapper;
import fr.airsen.api.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ForumThreadDTOIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private ForumMessageRepository messageRepository;

    @Autowired
    private ForumVoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumThreadMapper mapper;

    @Autowired
    private EntityManager entityManager;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User createUser(String username) {
        User u = new User();
        u.setFirstName(username);
        u.setLastName("Doe");
        u.setEmail(username + "@gmail.com");
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
        t.setContent("Content " + title);
        t.setAuthor(author);
        t.setCategory(category);
        t.setCreatedDate(LocalDateTime.now());
        t.setLastMessageDate(LocalDateTime.now());
        t.setViewCount(5);
        t.setPinned(false);
        t.setClosed(false);
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

    private ForumVote createVote(String value, User user, ForumThread thread) {
        ForumVote v = new ForumVote();
        v.setVoteType(VoteType.valueOf(value));
        v.setUser(user);
        v.setThread(thread);
        return voteRepository.save(v);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DTO constructor should correctly map fields with nested messages and votes")
    void testDTOConstructorWithEntities() {

        User author = createUser("alice");
        User voter1 = createUser("bob");
        User voter2 = createUser("charlie");

        ForumCategory category = createCategory("Science");
        ForumThread thread = createThread("Quantum Physics", author, category);

        // messages
        createMessage("First!", author, thread);
        createMessage("Interesting topic.", voter1, thread);

        // votes
        createVote("LIKE", voter1, thread);
        createVote("LIKE", voter2, thread);
        createVote("DISLIKE", author, thread);

        entityManager.flush();
        entityManager.clear();

        ForumThread reloaded = threadRepository.findByIdWithMessages(thread.getId()).orElseThrow();

        // create DTO with nested data
        ForumThreadDTO dto = new ForumThreadDTO(reloaded, true);

        assertThat(dto.getId()).isEqualTo(thread.getId());
        assertThat(dto.getTitle()).isEqualTo("Quantum Physics");
        assertThat(dto.getContent()).isEqualTo(thread.getContent());

        // nested author
        assertThat(dto.getAuthor().getFirstName()).isEqualTo("alice");

        // nested category
        assertThat(dto.getCategory().getName()).isEqualTo("Science");

        // nested messages
        assertThat(dto.getMessages()).hasSize(2);

        // nested votes
        assertThat(dto.getVotes()).hasSize(3);

        // computed fields
        assertThat(dto.getLikeCount()).isEqualTo(1); // +1 +1 -1 = 1
        assertThat(dto.getMessageCount()).isEqualTo(2);
    }


    @Test
    @DisplayName("DTO constructor should exclude entities when withEntities=false")
    void testDTOConstructorWithoutEntities() {

        User author = createUser("david");
        ForumCategory category = createCategory("Math");
        ForumThread thread = createThread("Algebra", author, category);

        createMessage("Hello", author, thread);
        createVote("LIKE", author, thread);

        entityManager.flush();
        entityManager.clear();

        ForumThread reloaded = threadRepository.findById(thread.getId()).orElseThrow();

        ForumThreadDTO dto = new ForumThreadDTO(reloaded, false);

        assertThat(dto.getMessages()).isNull();
        assertThat(dto.getVotes()).isNull();

        // still computed messageCount/likeCount
        assertThat(dto.getMessageCount()).isEqualTo(1);
        assertThat(dto.getLikeCount()).isEqualTo(1);
    }


    @Test
    @DisplayName("DTO → Entity using mapper should persist correctly")
    void testDTOToEntityPersistence() {

        ForumThreadDTO dto = new ForumThreadDTO();
        dto.setTitle("History Thread");
        dto.setContent("Discussing ancient civilizations");
        dto.setCreatedDate(LocalDateTime.now());
        dto.setLastMessageDate(LocalDateTime.now());
        dto.setViewCount(10);
        dto.setPinned(true);
        dto.setClosed(false);
        dto.setLikeCount(5);

        ForumThread entity = mapper.toEntity(dto);

        ForumThread saved = threadRepository.save(entity);

        ForumThread loaded = threadRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getTitle()).isEqualTo("History Thread");
        assertThat(loaded.getContent()).isEqualTo("Discussing ancient civilizations");
        assertThat(loaded.isPinned()).isTrue();
        assertThat(loaded.getViewCount()).isEqualTo(10);
    }


    @Test
    @DisplayName("Entity → DTO → Entity round trip should preserve core fields")
    void testRoundTripConversion() {

        User author = createUser("eva");
        ForumCategory category = createCategory("Art");

        ForumThread entity = createThread("Painting", author, category);
        entity.setPinned(true);
        entity.setClosed(true);
        threadRepository.save(entity);

        ForumThreadDTO dto = mapper.toDTO(entity);
        ForumThread roundTrip = mapper.toEntity(dto);

        assertThat(roundTrip.getTitle()).isEqualTo("Painting");
        assertThat(roundTrip.isPinned()).isEqualTo(entity.isPinned());
        assertThat(roundTrip.isClosed()).isEqualTo(entity.isClosed());
        assertThat(roundTrip.getContent()).isEqualTo(entity.getContent());
    }


    @Test
    @DisplayName("List<ForumThread> → List<ForumThreadDTO> mapping should work")
    void testListMapping() {

        User user = createUser("frank");
        ForumCategory cat = createCategory("Tech");

        createThread("AI", user, cat);
        createThread("Cybersecurity", user, cat);

        List<ForumThread> all = threadRepository.findAll();
        List<ForumThreadDTO> dtoList = mapper.toDTOs(all);

        assertThat(dtoList).hasSize(2);

        assertThat(dtoList)
                .extracting(ForumThreadDTO::getTitle)
                .containsExactlyInAnyOrder("AI", "Cybersecurity");
    }
}
