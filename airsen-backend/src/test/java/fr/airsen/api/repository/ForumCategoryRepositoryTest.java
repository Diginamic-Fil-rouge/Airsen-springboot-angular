package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ForumCategoryRepositoryTest {

    @Autowired
    private ForumCategoryRepository repository;

    @Test
    @DisplayName("Test saving and retrieving a ForumCategory")
    void testSaveAndFind() {
        ForumCategory category = new ForumCategory();
        category.setName("Technology");

        // Save entity
        ForumCategory savedCategory = repository.save(category);

        // Verify saved
        assertThat(savedCategory.getId()).isNotNull();

        // Retrieve by name
        ForumCategory retrieved = repository.findByName("Technology");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Technology");

        // Retrieve all
        List<ForumCategory> categories = repository.findAll();
        assertThat(categories).hasSize(1).contains(savedCategory);
    }

    @Test
    @DisplayName("Test deleting a ForumCategory by ID")
    void testDeleteById() {
        ForumCategory category = new ForumCategory();
        category.setName("Science");

        ForumCategory savedCategory = repository.save(category);

        // Verify saved
        assertThat(repository.findById(savedCategory.getId())).isPresent();

        // Delete
        repository.deleteById(savedCategory.getId());

        // Verify deletion
        Optional<ForumCategory> deleted = repository.findById(savedCategory.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Test findByName returns null if not found")
    void testFindByNameNotFound() {
        ForumCategory result = repository.findByName("NonExistingCategory");
        assertThat(result).isNull();
    }
}
