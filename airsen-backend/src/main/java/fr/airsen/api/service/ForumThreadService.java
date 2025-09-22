package fr.airsen.api.service;

import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.mapper.ForumThreadMapper;
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

    public List<ForumThreadDTO> findAll() {
        return mapper.toDTOs(forumThreadRepository.findAll());
    }

    public List<ForumThreadDTO> findByCategory(long id) throws EntityNotFoundException {
        ForumCategory category = forumCategoryRepository.findById(id).orElse(null);
        if (category == null){
            throw new EntityNotFoundException("Category not found");
        }
        return mapper.toDTOs(forumThreadRepository.findByCategory(category));
    }

    public List<ForumThreadDTO> findByAuthor(Long id) throws EntityNotFoundException {
        User author = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Author not found"));
        return mapper.toDTOs(forumThreadRepository.findByAuthor(author));
    }

    public ForumThreadDTO findById(long id) {
        return mapper.toDTO(forumThreadRepository.findById(id).orElse(null));
    }

    /**
     * Adds a new thread to a forum category with validation.
     * Validates thread content and ensures category exists before creation.
     */

    public List<ForumThreadDTO> addThreadToCategory(long categoryId, @RequestBody ForumThread forumThread, BindingResult result) throws EntityNotFoundException, IllegalArgumentException {
        if (result.hasErrors()) {
            throw new IllegalArgumentException("Invalid thread : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumCategory category = forumCategoryRepository.findById(categoryId).orElse(null);
        if (category == null)
        {
            throw new EntityNotFoundException("Category not found");
        }
        forumThread.setCategory(category);
        forumThreadRepository.save(forumThread);
        return mapper.toDTOs(forumThreadRepository.findAll());
    }

    /**
     * Updates an existing forum thread with validation.
     * Validates thread content and ensures thread exists before updating.
     */

    public ForumThreadDTO updateThread(long id, @RequestBody ForumThread forumThread, BindingResult result) throws EntityNotFoundException, IllegalArgumentException {
        if (result.hasErrors()) {
            throw new IllegalArgumentException("Invalid thread : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumThread entityExists = forumThreadRepository.findById(id).orElse(null);
        if (entityExists == null) {
            throw new EntityNotFoundException("Failed to update thread - Thread not found");
        }
        forumThreadRepository.save(forumThread);
        return mapper.toDTO(forumThread);
    }

    /**
     * Deletes a forum thread and returns updated thread list.
     * Ensures thread exists before deletion.
     */

    public List<ForumThreadDTO> deleteThread(long id) throws EntityNotFoundException {
        ForumThread entityExists = forumThreadRepository.findById(id).orElse(null);
        if (entityExists == null) {
            throw new EntityNotFoundException("Failed to delete thread - Thread not found");
        }
        forumThreadRepository.deleteById(id);
        return mapper.toDTOs(forumThreadRepository.findAll());
    }
}