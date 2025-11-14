package fr.airsen.api.dto;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.mapper.ForumCategoryMapper;
import fr.airsen.api.repository.ForumCategoryRepository;
import fr.airsen.api.repository.ForumThreadRepository;
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
class ForumCategoryDTOIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumCategoryMapper mapper;

    @Autowired
    private EntityManager entityManager;

    // -------------------------------
    // Helpers
    // -------------------------------

    private User createUser(String name) {
        User user = new User();
        user.setFirstName(name);
        user.setLastName("Doe");
        user.setEmail(name + "@gmail.com");
        user.setRole(UserRole.USER);
        user.setPassword("password");
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private ForumCategory createCategory(String name, String desc, String color) {
        ForumCategory cat = new ForumCategory();
        cat.setName(name);
        cat.setDescription(desc);
        cat.setColor(color);
        cat.setThreads(new ArrayList<>());
        return categoryRepository.save(cat);
    }

    private ForumThread createThread(String title, String content, User author, ForumCategory category) {
        ForumThread t = new ForumThread();
        t.setTitle(title);
        t.setContent(content);
        t.setAuthor(author);
        t.setCategory(category);
        t.setCreatedDate(LocalDateTime.now());
        return threadRepository.save(t);
    }

    // -------------------------------
    // Tests
    // -------------------------------

    @Test
    @DisplayName("DTO constructor should correctly map fields from entity (with threads enabled)")
    void testDTOConstructorWithThreads() {

        User author = createUser("alice");
        ForumCategory category = createCategory("Science", "All about science", "#FFAA00");

        ForumThread thread1 = createThread("Physics", "Quantum stuff", author, category);
        ForumThread thread2 = createThread("Biology", "Cells etc", author, category);
        entityManager.flush();
        entityManager.clear();
        ForumCategory reloaded = categoryRepository.findById(category.getId()).orElseThrow();

        // Test the DTO constructor directly
        ForumCategoryDTO dto = new ForumCategoryDTO(reloaded, true);

        assertThat(dto.getId()).isEqualTo(category.getId());
        assertThat(dto.getName()).isEqualTo("Science");
        assertThat(dto.getDescription()).isEqualTo("All about science");
        assertThat(dto.getColor()).isEqualTo("#FFAA00");

        // Threads should be included
        assertThat(dto.getThreads()).hasSize(2);

        assertThat(dto.getThreads())
                .extracting(ForumThreadDTO::getTitle)
                .containsExactlyInAnyOrder("Physics", "Biology");
    }


    @Test
    @DisplayName("DTO constructor should not load threads when withThreads = false")
    void testDTOConstructorWithoutThreads() {

        ForumCategory category = createCategory("Art", "Creative topics", "#ABCDEF");

        ForumCategory catReloaded = categoryRepository.findById(category.getId()).orElseThrow();

        ForumCategoryDTO dto = new ForumCategoryDTO(catReloaded, false);

        assertThat(dto.getThreads()).isEmpty();
    }


    @Test
    @DisplayName("DTO → Entity using mapper should persist correctly in DB")
    void testDTOToEntityPersistence() {

        ForumCategoryDTO dto = new ForumCategoryDTO();
        dto.setName("History");
        dto.setDescription("History discussions");
        dto.setColor("#123ABC");

        // Mapping is done by mapper (as required)
        ForumCategory entity = mapper.toEntity(dto);

        ForumCategory saved = categoryRepository.save(entity);

        assertThat(saved.getId()).isNotZero();

        ForumCategory loaded = categoryRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("History");
        assertThat(loaded.getDescription()).isEqualTo("History discussions");
        assertThat(loaded.getColor()).isEqualTo("#123ABC");
    }


    @Test
    @DisplayName("Entity → DTO → Entity round trip should keep identical data")
    void testRoundTripDTOConversion() {

        ForumCategory category = createCategory("Sports", "All sports talks", "#00CC99");

        ForumCategoryDTO dto = mapper.toDTO(category); // DTO creation tested here
        ForumCategory mappedBack = mapper.toEntity(dto);

        assertThat(mappedBack.getName()).isEqualTo(category.getName());
        assertThat(mappedBack.getDescription()).isEqualTo(category.getDescription());
        assertThat(mappedBack.getColor()).isEqualTo(category.getColor());
    }


    @Test
    @DisplayName("DTO list operations should work with database data")
    void testDTOListMapping() {

        // random color to test null handling
        createCategory("CatA", "DescA", "#000000");
        createCategory("CatB", "DescB", "#000000");

        List<ForumCategory> all = categoryRepository.findAll();

        List<ForumCategoryDTO> dtoList = mapper.toDTOs(all);

        assertThat(dtoList).hasSize(2);

        assertThat(dtoList)
                .extracting(ForumCategoryDTO::getName)
                .containsExactlyInAnyOrder("CatA", "CatB");
    }
}
