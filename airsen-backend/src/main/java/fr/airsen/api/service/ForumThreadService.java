package fr.airsen.api.service;

import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.dto.ForumThreadMapper;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.ForumCategoryRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
/**
 * Service class for managing forum threads.
 * This class provides a set of methods for creating, reading, updating and deleting forum threads.
 */
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

    /**
     * Finds all forum threads.
     *
     * @return List of {@link ForumThreadDTO}.
     */
    public List<ForumThreadDTO> findAll() {
        return mapper.toDTOs(forumThreadRepository.findAll());
    }

    /**
     * Finds all forum threads in a given category.
     *
     * @param id Category ID.
     * @return List of {@link ForumThreadDTO}.
     * @throws EntityNotFoundException if category with given ID is not found.
     */
    public List<ForumThreadDTO> findByCategory(long id) throws EntityNotFoundException {
        ForumCategory category = forumCategoryRepository.findById(id);
        if (category == null){
            throw new EntityNotFoundException("Category not found");
        }
        return mapper.toDTOs(forumThreadRepository.findByCategory(category));
    }

    /**
     * Finds all forum threads written by a given author.
     *
     * @param id Author ID.
     * @return List of {@link ForumThreadDTO}.
     * @throws EntityNotFoundException if author with given ID is not found.
     */
    public List<ForumThreadDTO> findByAuthor(int id) throws EntityNotFoundException {
        User author = userRepository.findById(id);
        if (author == null){
            throw new EntityNotFoundException("Author not found");
        }
        return mapper.toDTOs(forumThreadRepository.findByAuthor(author));
    }

    /**
     * Finds a forum thread by its ID.
     *
     * @param id Thread ID.
     * @return The {@link ForumThreadDTO} with the specified ID.
     */
    public ForumThreadDTO findById(long id) {
        return mapper.toDTO(forumThreadRepository.findById(id));
    }

    /**
     * Adds a new forum thread to a category.
     *
     * @param categoryId  The ID of the category to which the thread will be added.
     * @param forumThread Forum thread to be added.
     * @return List of all forum threads as {@link ForumThreadDTO}.
     * @throws EntityNotFoundException if category with given ID is not found.
     */
    public List<ForumThreadDTO> addThreadToCategory(long categoryId, @RequestBody ForumThread forumThread, BindingResult result) throws EntityNotFoundException, IllegalArgumentException {
        if (result.hasErrors()) {
            throw new IllegalArgumentException("Invalid thread : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumCategory category = forumCategoryRepository.findById(categoryId);
        if (category == null)
        {
            throw new EntityNotFoundException("Category not found");
        }
        forumThread.setCategory(category);
        forumThreadRepository.save(forumThread);
        return mapper.toDTOs(forumThreadRepository.findAll());
    }

    /**
     * Updates a forum thread.
     *
     * @param id          the ID of the thread to be updated.
     * @param forumThread Forum thread to be updated.
     * @return The updated forum thread as {@link ForumThreadDTO}.
     */
    public ForumThreadDTO updateThread(long id, @RequestBody ForumThread forumThread, BindingResult result) throws EntityNotFoundException, IllegalArgumentException {
        if (result.hasErrors()) {
            throw new IllegalArgumentException("Invalid thread : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumThread entityExists = forumThreadRepository.findById(id);
        if (entityExists == null) {
            throw new EntityNotFoundException("Failed to update thread - Thread not found");
        }
        forumThreadRepository.save(forumThread);
        return mapper.toDTO(forumThread);
    }

    /**
     * Deletes a forum thread.
     *
     * @param id The ID of the thread to be deleted.
     * @return List of all forum threads as {@link ForumThreadDTO}.
     * @throws EntityNotFoundException If the thread is not found.
     */
    public List<ForumThreadDTO> deleteThread(long id) throws EntityNotFoundException {
        ForumThread entityExists = forumThreadRepository.findById(id);
        if (entityExists == null) {
            throw new EntityNotFoundException("Failed to delete thread - Thread not found");
        }
        forumThreadRepository.deleteById(id);
        return mapper.toDTOs(forumThreadRepository.findAll());
    }
}