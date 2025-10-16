package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link ForumCategory} entities.
 */
public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {

    /**
     * Find all {@link ForumCategory} entities.
     *
     * @return list of {@link ForumCategory} entities
     */
    List<ForumCategory> findAll();


    /**
     * Find {@link ForumCategory} entity by its name.
     *
     * @param name Name of the {@link ForumCategory} entity
     * @return {@link ForumCategory} entity with the given name or {@code null} if not found
     */
    ForumCategory findByName(String name);

    /**
     * Delete {@link ForumCategory} entity by ID.
     *
     * @param id ID of the {@link ForumCategory} entity
     */
    void deleteById(Long id);
}
