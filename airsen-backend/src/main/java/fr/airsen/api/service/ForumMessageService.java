package fr.airsen.api.service;

import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.dto.request.ForumMessageCreateRequest;
import fr.airsen.api.dto.request.ForumMessageUpdateRequest;
import fr.airsen.api.mapper.ForumMessageMapper;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.ForumMessageRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.security.UserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class ForumMessageService {

    @Autowired
    private ForumMessageRepository forumMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumThreadRepository forumThreadRepository;

    @Autowired
    private ForumMessageMapper forumMessageMapper;

    @Transactional
    public List<ForumMessageDTO> findAll() {
        return forumMessageMapper.toDTOs(forumMessageRepository.findAll());
    }

    @Transactional
    public ForumMessageDTO findById(long id) {
        return forumMessageMapper.toDTO(forumMessageRepository.findById(id).orElse(null));
    }

    @Transactional
    public List<ForumMessageDTO> findByAuthor(Long id) throws EntityNotFoundException {
        User author = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Author not found"));
        return forumMessageMapper.toDTOs(forumMessageRepository.findByAuthor(author));
    }

    @Transactional
    public List<ForumMessageDTO> getMessagesByThread(long id) throws EntityNotFoundException {
        ForumThread thread = forumThreadRepository.findById(id).orElse(null);
        if (thread == null) {
            throw new EntityNotFoundException("Thread not found");
        }
        return forumMessageMapper.toDTOs(forumMessageRepository.findByThread(thread));
    }

    /**
     * Adds a new message to a forum thread from a request DTO.
     *
     * @param threadId the thread ID to add the message to
     * @param request  the message creation request containing content
     * @return the created message DTO
     * @throws EntityNotFoundException if thread or user not found
     */
    @Transactional
    public ForumMessageDTO addMessageToThread(long threadId, ForumMessageCreateRequest request) throws EntityNotFoundException {
        // Find thread
        ForumThread thread = forumThreadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found with ID: " + threadId));

        // Get authenticated user
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + principal.getId()));

        // Create message entity
        ForumMessage message = new ForumMessage();
        message.setContent(request.getContent());
        message.setAuthor(author);
        message.setThread(thread);
        message.setCreatedDate(LocalDateTime.now());

        // Save and return DTO
        ForumMessage savedMessage = forumMessageRepository.save(message);

        // Update thread's last message date
        thread.setLastMessageDate(LocalDateTime.now());
        forumThreadRepository.save(thread);

        return forumMessageMapper.toDTO(savedMessage);
    }

    /**
     * Updates an existing forum message from a request DTO.
     *
     * @param id      the message ID to update
     * @param request the message update request containing new content
     * @return the updated message DTO
     * @throws EntityNotFoundException if message not found
     */
    @Transactional
    public ForumMessageDTO updateMessage(long id, ForumMessageUpdateRequest request) throws EntityNotFoundException {
        // Find existing message
        ForumMessage message = forumMessageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + id));

        // Update content
        message.setContent(request.getContent());

        // Save and return DTO
        ForumMessage updatedMessage = forumMessageRepository.save(message);
        return forumMessageMapper.toDTO(updatedMessage);
    }

    @Transactional
    public void deleteMessage(long id) throws EntityNotFoundException {
        ForumMessage message = forumMessageRepository.findById(id).orElse(null);
        if (message == null) {
            throw new EntityNotFoundException("Message not found");
        }

        if (message.getAuthor() != null && message.getAuthor().getMessages() != null) {
            message.getAuthor().getMessages().remove(message);
        }
        if (message.getThread() != null && message.getThread().getMessages() != null) {
            message.getThread().getMessages().remove(message);
        }

        forumMessageRepository.deleteById(id);
    }
}