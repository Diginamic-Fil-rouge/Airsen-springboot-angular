package fr.airsen.api.service;

import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.mapper.ForumMessageMapper;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.ForumMessageRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

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

    public List<ForumMessageDTO> findAll() {
        return forumMessageMapper.toDTOs(forumMessageRepository.findAll());
    }

    public ForumMessageDTO findById(long id) {
        return forumMessageMapper.toDTO(forumMessageRepository.findById(id).orElse(null));
    }

    public List<ForumMessageDTO> findByAuthor(Long id) throws EntityNotFoundException{
        User author = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Author not found"));
        return forumMessageMapper.toDTOs(forumMessageRepository.findByAuthor(author));
    }

    public List<ForumMessageDTO> getMessagesByThread(long id) throws EntityNotFoundException{
        ForumThread thread = forumThreadRepository.findById(id).orElse(null);
        if (thread == null)
        {
            throw new EntityNotFoundException("Thread not found");
        }
        return forumMessageMapper.toDTOs(forumMessageRepository.findByThread(thread));
    }

    /**
     * Adds a new message to a forum thread with validation.
     * Validates message content and ensures thread exists before adding the message.
     */

    public ForumMessageDTO addMessageToThread(long id, @RequestBody ForumMessage forumMessage, BindingResult result) throws EntityNotFoundException, IllegalArgumentException{
        if (result.hasErrors()){
            throw new IllegalArgumentException("Invalid message : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumThread thread = forumThreadRepository.findById(id).orElse(null);
        if (thread == null)
        {
            throw new EntityNotFoundException("Thread not found - Cannot add message");
        }
        User user = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        forumMessage.setAuthor(user);
        forumMessage.setThread(thread);
        return forumMessageMapper.toDTO(forumMessageRepository.save(forumMessage));
    }

    /**
     * Updates an existing forum message with validation.
     * Validates message content and ensures message exists before updating.
     */

    public ForumMessageDTO updateMessage(long id, @RequestBody ForumMessage forumMessage, BindingResult result) throws EntityNotFoundException, IllegalArgumentException{
        if (result.hasErrors()){
            throw new IllegalArgumentException("Invalid message : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumMessage message = forumMessageRepository.findById(id).orElse(null);
        if (message == null)
        {
            throw new EntityNotFoundException("Message not found");
        }
        message.setAuthor(forumMessage.getAuthor());
        message.setThread(forumMessage.getThread());
        message.setContent(forumMessage.getContent());
        return forumMessageMapper.toDTO(forumMessageRepository.save(message));
    }

    public void deleteMessage(long id) throws EntityNotFoundException{
        ForumMessage message = forumMessageRepository.findById(id).orElse(null);
        if (message == null)
        {
            throw new EntityNotFoundException("Message not found");
        }
        forumMessageRepository.deleteById(id);
    }
}