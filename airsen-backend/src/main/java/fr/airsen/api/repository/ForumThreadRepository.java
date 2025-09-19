package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link ForumThread} entity.
 */
public interface ForumThreadRepository extends JpaRepository<ForumThread, Long> {

    /**
     * Find all {@link ForumThread} entities.
     *
     * @return list of {@link ForumThread} entities
     */
    List<ForumThread> findAll();


    /**
     * Find {@link ForumThread} entities by {@link ForumCategory}.
     *
     * @param category {@link ForumCategory} entity
     * @return list of {@link ForumThread} entities with the specified {@link ForumCategory}
     */
    List<ForumThread> findByCategory(ForumCategory category);

    /**
     * Find {@link ForumThread} entities by {@link User}.
     *
     * @param author {@link User} entity
     * @return list of {@link ForumThread} entities with the specified {@link User}
     */
    List<ForumThread> findByAuthor(User author);

    /**
     * Delete thread with the corresponding id.
     * @param id id of the thread to delete
     */
    void deleteById(Long id);
}
