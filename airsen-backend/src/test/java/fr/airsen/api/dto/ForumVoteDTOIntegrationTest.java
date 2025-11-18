package fr.airsen.api.dto;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.*;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.entity.enums.VoteType;
import fr.airsen.api.mapper.ForumVoteMapper;
import fr.airsen.api.repository.ForumCategoryRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.ForumVoteRepository;
import fr.airsen.api.repository.UserRepository;

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
class ForumVoteDTOIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumVoteRepository voteRepository;

    @Autowired
    private ForumVoteMapper mapper;

    @Autowired
    private EntityManager entityManager;

    // --------------------------
    // Helper Methods
    // --------------------------

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

    private ForumVote createVote(User user, ForumThread thread, VoteType type) {
        ForumVote v = new ForumVote();
        v.setUser(user);
        v.setThread(thread);
        v.setVoteType(type);
        return voteRepository.save(v);
    }

    // --------------------------
    // Tests
    // --------------------------

    @Test
    @DisplayName("DTO constructor withEntities=true maps nested objects correctly")
    void testDTOConstructorWithEntities() {

        User user = createUser("alice");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread A", user, category);
        ForumVote vote = createVote(user, thread, VoteType.LIKE);

        entityManager.flush();
        entityManager.clear();

        ForumVote loaded = voteRepository.findById(vote.getId()).orElseThrow();

        ForumVoteDTO dto = new ForumVoteDTO(loaded, true);

        assertThat(dto.getId()).isEqualTo(vote.getId());
        assertThat(dto.getVoteType()).isEqualTo(VoteType.LIKE);

        assertThat(dto.getUser()).isNotNull();
        assertThat(dto.getUser().getFirstName()).isEqualTo("alice");

        assertThat(dto.getThread()).isNotNull();
        assertThat(dto.getThread().getId()).isEqualTo(thread.getId());
    }

    @Test
    @DisplayName("DTO constructor withEntities=false excludes user & thread")
    void testDTOConstructorWithoutEntities() {

        User user = createUser("bob");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread B", user, category);
        ForumVote vote = createVote(user, thread, VoteType.DISLIKE);

        ForumVote loaded = voteRepository.findById(vote.getId()).orElseThrow();

        ForumVoteDTO dto = new ForumVoteDTO(loaded, false);

        assertThat(dto.getUser()).isNull();
        assertThat(dto.getThread()).isNull();
        assertThat(dto.getVoteType()).isEqualTo(VoteType.DISLIKE);
    }

    @Test
    @DisplayName("DTO → Entity mapping persists correctly to database")
    void testDTOToEntityPersistence() {

        User user = createUser("charlie");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread C", user, category);

        ForumVoteDTO dto = new ForumVoteDTO();
        dto.setVoteType(VoteType.LIKE);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        dto.setUser(userDTO);

        // Map DTO → Entity
        ForumVote entity = mapper.toEntity(dto);
        entity.setUser(user);     // Mapper does NOT set these
        entity.setThread(thread); // must set manually

        ForumVote saved = voteRepository.save(entity);

        ForumVote loaded = voteRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getVoteType()).isEqualTo(VoteType.LIKE);
        assertThat(loaded.getUser().getId()).isEqualTo(user.getId());
        assertThat(loaded.getThread().getId()).isEqualTo(thread.getId());
    }

    @Test
    @DisplayName("Entity → DTO → Entity round-trip preserves values")
    void testRoundTrip() {

        User user = createUser("dave");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread D", user, category);
        ForumVote vote = createVote(user, thread, VoteType.DISLIKE);

        ForumVoteDTO dto = mapper.toDTO(vote);
        ForumVote mappedBack = mapper.toEntity(dto);

        mappedBack.setUser(user);     // required
        mappedBack.setThread(thread); // required

        assertThat(mappedBack.getVoteType()).isEqualTo(vote.getVoteType());
        assertThat(mappedBack.getUser().getId()).isEqualTo(vote.getUser().getId());
    }

    @Test
    @DisplayName("List mapping Entity→DTO works correctly")
    void testListMapping() {

        User user = createUser("eve");
        ForumCategory category = createCategory("test");
        ForumThread thread = createThread("Thread E", user, category);

        createVote(user, thread, VoteType.LIKE);
        createVote(user, thread, VoteType.DISLIKE);

        List<ForumVote> votes = voteRepository.findAll();
        List<ForumVoteDTO> dtoList = mapper.toDTOs(votes);

        assertThat(dtoList).hasSize(2);
        assertThat(dtoList)
                .extracting(ForumVoteDTO::getVoteType)
                .containsExactlyInAnyOrder(VoteType.LIKE, VoteType.DISLIKE);
    }
}
