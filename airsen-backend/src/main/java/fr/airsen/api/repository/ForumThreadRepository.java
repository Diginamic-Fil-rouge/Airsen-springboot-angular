package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
     * Find all {@link ForumThread} entities with related data eagerly loaded.
     *
     * @return list of {@link ForumThread} entities with related data loaded
     */
    @Query("SELECT DISTINCT t FROM ForumThread t " +
           "LEFT JOIN FETCH t.author " +
           "LEFT JOIN FETCH t.category " +
           "ORDER BY t.createdDate DESC")
    List<ForumThread> findAllWithRelations();


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
     * Find {@link ForumThread} by ID with messages eagerly loaded.
     * 
     * @param id thread ID
     * @return Optional of {@link ForumThread} with messages loaded
     */
    @Query("SELECT t FROM ForumThread t " +
           "LEFT JOIN FETCH t.messages m " +
           "LEFT JOIN FETCH t.author " +
           "LEFT JOIN FETCH t.category " +
           "WHERE t.id = :id")
    Optional<ForumThread> findByIdWithMessages(@Param("id") Long id);

    /**
     * Find {@link ForumThread} entities by {@link ForumCategory} with messages eagerly loaded.
     * 
     * @param category {@link ForumCategory} entity
     * @return list of {@link ForumThread} entities with messages loaded
     */
    @Query("SELECT t FROM ForumThread t " +
           "LEFT JOIN FETCH t.messages m " +
           "LEFT JOIN FETCH t.author " +
           "LEFT JOIN FETCH t.category " +
           "WHERE t.category = :category")
    List<ForumThread> findByCategoryWithMessages(@Param("category") ForumCategory category);

    /**
     * Delete thread with the corresponding id.
     * @param id id of the thread to delete
     */
    void deleteById(Long id);

    /**
     * Find threads by category with pagination.
     *
     * @param categoryId category ID
     * @param pageable pagination parameters
     * @return page of threads
     */
    Page<ForumThread> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * Search threads by title or content with pagination.
     *
     * @param search1 search term for title
     * @param search2 search term for content (same as search1)
     * @param pageable pagination parameters
     * @return page of threads
     */
    @Query("SELECT t FROM ForumThread t WHERE " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.content) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ForumThread> findByTitleContainingOrContentContaining(
        @Param("search") String search1,
        @Param("search") String search2,
        Pageable pageable
    );

    /**
     * Find threads by category and search with pagination.
     *
     * @param categoryId category ID
     * @param search1 search term for title
     * @param search2 search term for content (same as search1)
     * @param pageable pagination parameters
     * @return page of threads
     */
    @Query("SELECT t FROM ForumThread t WHERE t.category.id = :categoryId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ForumThread> findByCategoryIdAndTitleContainingOrContentContaining(
        @Param("categoryId") Long categoryId,
        @Param("search") String search1,
        @Param("search") String search2,
        Pageable pageable
    );
}
