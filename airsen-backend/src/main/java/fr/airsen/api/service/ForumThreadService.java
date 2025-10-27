package fr.airsen.api.service;

import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.dto.request.ForumThreadCreateRequest;
import fr.airsen.api.dto.request.ForumThreadUpdateRequest;
import fr.airsen.api.mapper.ForumThreadMapper;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.ForumCategoryRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.security.UserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ForumThreadService {

    @Autowired
    private ForumThreadRepository forumThreadRepository;

    @Autowired
    private ForumCategoryRepository forumCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumThreadMapper mapper;

    @Transactional(readOnly = true)
    public List<ForumThreadDTO> findAll() {
        return mapper.toDTOs(forumThreadRepository.findAllWithRelations());
    }

    /**
     * Find all threads with pagination, filtering, and sorting.
     *
     * @param categoryId optional category filter
     * @param search optional text search in title/content
     * @param pageable pagination and sorting parameters
     * @return page of forum threads
     */
    @Transactional(readOnly = true)
    public Page<ForumThreadDTO> findAll(Long categoryId, String search, Pageable pageable) {
        Page<ForumThread> threadsPage;

        if (categoryId != null && search != null && !search.trim().isEmpty()) {
            // Filter by category AND search
            threadsPage = forumThreadRepository
                .findByCategoryIdAndTitleContainingOrContentContaining(
                    categoryId, search, search, pageable);
        } else if (categoryId != null) {
            // Filter by category only
            threadsPage = forumThreadRepository.findByCategoryId(categoryId, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            // Search only
            threadsPage = forumThreadRepository
                .findByTitleContainingOrContentContaining(search, search, pageable);
        } else {
            // No filters
            threadsPage = forumThreadRepository.findAll(pageable);
        }

        // Convert to DTOs
        return threadsPage.map(thread -> new ForumThreadDTO(thread, false));
    }

    @Transactional(readOnly = true)
    public List<ForumThreadDTO> findByCategory(Long id) throws EntityNotFoundException {
        ForumCategory category = forumCategoryRepository.findById(id).orElse(null);
        if (category == null){
            throw new EntityNotFoundException("Category not found");
        }
        // Use custom query with JOIN FETCH to eagerly load messages
        return mapper.toDTOs(forumThreadRepository.findByCategoryWithMessages(category));
    }

    public List<ForumThreadDTO> findByAuthor(Long id) throws EntityNotFoundException {
        User author = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Author not found"));
        return mapper.toDTOs(forumThreadRepository.findByAuthor(author));
    }

    @Transactional(readOnly = true)
    public ForumThreadDTO findById(Long id) {
        // Use custom query with JOIN FETCH to eagerly load messages
        ForumThread thread = forumThreadRepository.findByIdWithMessages(id).orElse(null);
        if (thread != null) {
            thread.setViewCount(thread.getViewCount() + 1);
            forumThreadRepository.save(thread);
        }
        return mapper.toDTO(thread);
    }

    /**
     * Create a new forum thread from a request DTO.
     *
     * @param request the thread creation request containing title, content, and categoryId
     * @return the created thread DTO
     * @throws EntityNotFoundException if user or category not found
     */
    @Transactional
    public ForumThreadDTO createThread(ForumThreadCreateRequest request) throws EntityNotFoundException {
        // Get authenticated user from security context
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

        User author = userRepository.findById(principal.getId())
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + principal.getId()));

        // Get category by ID from request
        ForumCategory category = forumCategoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Category not found with ID: " + request.getCategoryId()));

        // Create new thread entity
        ForumThread thread = new ForumThread();
        thread.setTitle(request.getTitle());
        thread.setContent(request.getContent());
        thread.setAuthor(author);
        thread.setCategory(category);

        // Set default values for system fields
        LocalDateTime now = LocalDateTime.now();
        thread.setCreatedDate(now);
        thread.setLastMessageDate(now);
        thread.setViewCount(0);
        thread.setLikeCount(0);
        thread.setPinned(false);
        thread.setClosed(false);

        // Save to database
        ForumThread savedThread = forumThreadRepository.save(thread);

        // Return DTO
        return new ForumThreadDTO(savedThread, false);
    }

    @Transactional
    public List<ForumThreadDTO> addThreadToCategory(Long categoryId, ForumThreadCreateRequest request) throws EntityNotFoundException {
        ForumCategory category = forumCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException("Category not found - Cannot add thread"));

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ForumThread thread = new ForumThread();
        thread.setTitle(request.getTitle());
        thread.setContent(request.getContent());
        thread.setAuthor(user);
        thread.setCategory(category);

        LocalDateTime now = LocalDateTime.now();
        thread.setCreatedDate(now);
        thread.setLastMessageDate(now);
        thread.setViewCount(0);
        thread.setLikeCount(0);
        thread.setPinned(false);
        thread.setClosed(false);

        forumThreadRepository.save(thread);
        return mapper.toDTOs(forumThreadRepository.findAll());
    }

    /**
     * Update an existing forum thread from a request DTO.
     *
     * @param id the thread ID to update
     * @param request the thread update request containing title, content, and optional categoryId
     * @return the updated thread DTO
     * @throws EntityNotFoundException if thread or category not found
     */
    @Transactional
    public ForumThreadDTO updateThread(Long id, ForumThreadUpdateRequest request) throws EntityNotFoundException {
        // Find existing thread
        ForumThread thread = forumThreadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found with ID: " + id));

        // Update title and content
        thread.setTitle(request.getTitle());
        thread.setContent(request.getContent());

        // Update category if provided
        if (request.getCategoryId() != null) {
            ForumCategory newCategory = forumCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Category not found with ID: " + request.getCategoryId()));
            thread.setCategory(newCategory);
        }

        // Save updated thread
        ForumThread updatedThread = forumThreadRepository.save(thread);

        // Return DTO
        return new ForumThreadDTO(updatedThread, false);
    }

    @Transactional
    public void deleteThread(Long id) throws EntityNotFoundException {
        ForumThread thread = forumThreadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found"));

        // Remove from parent collections if they exist
        if (thread.getAuthor() != null && thread.getAuthor().getThreads() != null) {
            thread.getAuthor().getThreads().remove(thread);
        }

        if (thread.getCategory() != null && thread.getCategory().getThreads() != null) {
            thread.getCategory().getThreads().remove(thread);
        }

        forumThreadRepository.delete(thread);
    }
}