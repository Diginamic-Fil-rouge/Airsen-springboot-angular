package fr.airsen.api.service;

import fr.airsen.api.dto.ForumThreadDTO;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public List<ForumThreadDTO> findAll() {
        return mapper.toDTOs(forumThreadRepository.findAllWithRelations());
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
        return mapper.toDTO(forumThreadRepository.findByIdWithMessages(id).orElse(null));
    }

    @Transactional
    public ForumThreadDTO createThread(@RequestBody ForumThread forumThread, BindingResult result) throws EntityNotFoundException, IllegalArgumentException {
        if (result.hasErrors()) {
            throw new IllegalArgumentException("Invalid thread: " + result.getAllErrors().get(0).getDefaultMessage());
        }
        
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        ForumCategory category = forumCategoryRepository.findById(forumThread.getCategory().getId())
            .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        
        forumThread.setAuthor(user);
        forumThread.setCategory(category);
        
        // Set default values for required fields
        if (forumThread.getCreatedDate() == null) {
            forumThread.setCreatedDate(java.time.LocalDateTime.now());
        }
        if (forumThread.getLastMessageDate() == null) {
            forumThread.setLastMessageDate(forumThread.getCreatedDate());
        }
        if (forumThread.getViewCount() == null) {
            forumThread.setViewCount(0);
        }
        if (forumThread.getLikeCount() == null) {
            forumThread.setLikeCount(0);
        }
        
        ForumThread savedThread = forumThreadRepository.save(forumThread);
        
        return mapper.toDTO(savedThread);
    }

    @Transactional
    public List<ForumThreadDTO> addThreadToCategory(Long categoryId, @RequestBody ForumThread forumThread, BindingResult result) throws EntityNotFoundException, IllegalArgumentException {
        if (result.hasErrors()) {
            throw new IllegalArgumentException("Invalid thread : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumCategory category = forumCategoryRepository.findById(categoryId).orElse(null);
        System.out.println("Category found");
        if (category == null)
        {
            throw new EntityNotFoundException("Category not found - Cannot add thread");
        }
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        System.out.println("User found");
        forumThread.setAuthor(user);
        System.out.println("Author set");
        forumThread.setCategory(category);
        System.out.println("Category set");
        forumThreadRepository.save(forumThread);
        System.out.println("Thread saved");
        return mapper.toDTOs(forumThreadRepository.findAll());
    }

    @Transactional
    public ForumThreadDTO updateThread(Long id, @RequestBody ForumThread forumThread, BindingResult result) throws EntityNotFoundException, IllegalArgumentException {
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

    @Transactional
    public List<ForumThreadDTO> deleteThread(Long id) throws EntityNotFoundException {
        ForumThread entityExists = forumThreadRepository.findById(id).orElse(null);
        if (entityExists == null) {
            throw new EntityNotFoundException("Failed to delete thread - Thread not found");
        }
        forumThreadRepository.deleteById(id);
        return mapper.toDTOs(forumThreadRepository.findAll());
    }
}