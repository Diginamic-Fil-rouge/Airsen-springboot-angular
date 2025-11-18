package fr.airsen.api.repository;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.entity.ForumCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ForumCategoryRepositoryIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private ForumCategoryRepository repository;

    @Test
    @DisplayName("Should save a ForumCategory and assign an ID")
    void testSave() {
        try {
            ForumCategory category = new ForumCategory();
            category.setName("General");
            category.setColor("#FF0000");
            category.setDescription("Description");

            System.out.println("Before save - Category ID: " + category.getId());

            ForumCategory saved = repository.save(category);
            System.out.println("After save - Saved ID: " + saved.getId());

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("General");
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("Should return all ForumCategory entities")
    void testFindAll() {
        ForumCategory c1 = new ForumCategory();
        c1.setName("Tech");
        c1.setColor("#FF0000");
        c1.setDescription("Description");

        ForumCategory c2 = new ForumCategory();
        c2.setName("Sports");
        c2.setColor("#00FF00");
        c2.setDescription("Description");

        repository.save(c1);
        repository.save(c2);

        List<ForumCategory> all = repository.findAll();

        assertThat(all)
                .hasSize(2)
                .extracting(ForumCategory::getName)
                .containsExactlyInAnyOrder("Tech", "Sports");
    }

    @Test
    @DisplayName("Should find a ForumCategory by name")
    void testFindByName() {
        ForumCategory category = new ForumCategory();
        category.setName("News");
        category.setColor("#FF0000");
        category.setDescription("Description");
        repository.save(category);

        ForumCategory found = repository.findByName("News");

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("News");
    }

    @Test
    @DisplayName("Should delete a ForumCategory by ID")
    void testDeleteById() {
        ForumCategory category = new ForumCategory();
        category.setName("ToDelete");
        category.setColor("#FF0000");
        category.setDescription("Description");
        ForumCategory saved = repository.save(category);

        Long id = saved.getId();
        assertThat(id).isNotNull();

        repository.deleteById(id);

        ForumCategory afterDelete = repository.findById(id).orElse(null);

        assertThat(afterDelete).isNull();
    }
}
