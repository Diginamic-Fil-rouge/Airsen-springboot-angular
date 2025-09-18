package fr.airsen.api.service;

import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.dto.ForumMessageMapper;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.ForumMessageRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Service for handling forum messages.
 */
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

    /**
     * Find all forum messages.
     *
     * @return List of forum {@link ForumMessageDTO}.
     */
    public List<ForumMessageDTO> findAll() {
        return forumMessageMapper.toDTOs(forumMessageRepository.findAll());
    }

    /**
     * Find forum message by ID.
     *
     * @param id ID of the forum message.
     * @return {@link ForumMessageDTO}.
     */
    public ForumMessageDTO findById(long id) {
        return forumMessageMapper.toDTO(forumMessageRepository.findById(id));
    }

    /**
     * Find forum messages by author ID.
     *
     * @param id ID of the author.
     * @return List of {@link ForumMessageDTO}.
     * @throws EntityNotFoundException if author with given ID is not found.
     */
    public List<ForumMessageDTO> findByAuthor(int id) throws EntityNotFoundException{
        User author = userRepository.findById(id);
        if (author == null)
        {
            throw new EntityNotFoundException("Author not found");
        }
        return forumMessageMapper.toDTOs(forumMessageRepository.findByAuthor(author));
    }

    /**
     * Get forum messages by thread ID.
     *
     * @param id ID of the thread.
     * @return List of {@link ForumMessageDTO}.
     * @throws EntityNotFoundException if thread with given ID is not found.
     */
    public List<ForumMessageDTO> getMessagesByThread(long id) throws EntityNotFoundException{
        ForumThread thread = forumThreadRepository.findById(id);
        if (thread == null)
        {
            throw new EntityNotFoundException("Thread not found");
        }
        return forumMessageMapper.toDTOs(forumMessageRepository.findByThread(thread));
    }

    /**
     * Add a forum message to a thread.
     *
     * @param id         ID of the thread.
     * @param forumMessage Forum message to add.
     * @return {@link ForumMessageDTO}.
     * @throws EntityNotFoundException if forum thread with given ID is not found.
     */
    public ForumMessageDTO addMessageToThread(long id, @RequestBody ForumMessage forumMessage, BindingResult result) throws EntityNotFoundException, IllegalArgumentException{
        if (result.hasErrors()){
            throw new IllegalArgumentException("Invalid message : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumThread thread = forumThreadRepository.findById(id);
        if (thread == null)
        {
            throw new EntityNotFoundException("Message not found");
        }
        forumMessage.setThread(thread);
        return forumMessageMapper.toDTO(forumMessageRepository.save(forumMessage));
    }

    /**
     * Update a forum message.
     *
     * @param id         ID of the forum message.
     * @param forumMessage Forum message to update.
     * @return {@link ForumMessageDTO}.
     * @throws EntityNotFoundException if forum message with given ID is not found.
     */
    public ForumMessageDTO updateMessage(long id, @RequestBody ForumMessage forumMessage, BindingResult result) throws EntityNotFoundException, IllegalArgumentException{
        if (result.hasErrors()){
            throw new IllegalArgumentException("Invalid message : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumMessage message = forumMessageRepository.findById(id);
        if (message == null)
        {
            throw new EntityNotFoundException("Message not found");
        }
        message.setAuthor(forumMessage.getAuthor());
        message.setThread(forumMessage.getThread());
        message.setContent(forumMessage.getContent());
        return forumMessageMapper.toDTO(forumMessageRepository.save(message));
    }

    /**
     * Delete a forum message.
     *
     * @param id ID of the forum message.
     * @throws EntityNotFoundException if forum message with given ID is not found.
     */
    public void deleteMessage(long id) throws EntityNotFoundException{
        ForumMessage message = forumMessageRepository.findById(id);
        if (message == null)
        {
            throw new EntityNotFoundException("Message not found");
        }
        forumMessageRepository.deleteById(id);
    }
}