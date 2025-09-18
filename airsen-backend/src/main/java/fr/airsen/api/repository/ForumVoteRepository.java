package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link ForumVote}.
 */
public interface ForumVoteRepository extends JpaRepository<ForumVote, Integer> {

    /**
     * Find all {@link ForumVote} entities.
     *
     * @return list of {@link ForumVote} entities
     */
    List<ForumVote> findAll();

    /**
     * Find {@link ForumVote} by ID.
     *
     * @param id ID of the forum vote
     * @return {@link ForumVote} with the specified ID
     */
    ForumVote findById(long id);

    /**
     * Find {@link ForumVote} by {@link ForumThread}.
     *
     * @param thread {@link ForumThread} entity
     * @return list of {@link ForumVote} entities
     */
    List<ForumVote> findByThread(ForumThread thread);

    /**
     * Find {@link ForumVote} by {@link User} and {@link ForumThread}.
     *
     * @param user {@link User} entity
     * @param thread {@link ForumThread} entity
     * @return {@link ForumVote} entity
     */
    ForumVote findByUserAndThread(User user, ForumThread thread);
}
