package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link ForumMessage} JPA entities.
 */
public interface ForumMessageRepository extends JpaRepository<ForumMessage, Long> {

    /**
     * Get all {@link ForumMessage} entities.
     *
     * @return all {@link ForumMessage} entities.
     */
    List<ForumMessage> findAll();


    /**
     * Get all {@link ForumMessage} entities by {@link ForumThread}.
     *
     * @param thread {@link ForumThread} entity.
     * @return all {@link ForumMessage} entities by {@link ForumThread}.
     */
    List<ForumMessage> findByThread(ForumThread thread);

    /**
     * Get all {@link ForumMessage} entities by {@link User}.
     *
     * @param author {@link User} entity.
     * @return all {@link ForumMessage} entities by {@link User}.
     */
    List<ForumMessage> findByAuthor(User author);

    /**
     * Delete message by Id.
     * @param id id of the message to delete.
     */
    void deleteById(Long id);

    /**
     * Count messages posted by a specific author.
     *
     * @param authorId author user ID
     * @return number of messages posted by the author
     */
    long countByAuthorId(Long authorId);
}
