package fr.airsen.api.service;

import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.dto.ForumVoteDTO;
import fr.airsen.api.mapper.ForumThreadMapper;
import fr.airsen.api.mapper.ForumVoteMapper;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.VoteType;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.ForumVoteRepository;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.security.UserPrincipal;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ForumVoteService {
    
    @Autowired
    private ForumVoteRepository forumVoteRepository;
    
    @Autowired
    private ForumVoteMapper forumVoteMapper;
    
    @Autowired
    private ForumThreadMapper forumThreadMapper;
    
    @Autowired
    private ForumThreadRepository forumThreadRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ForumVoteDTO> findAllVoteByThread(Long id) throws EntityNotFoundException
    {
        ForumThread thread = forumThreadRepository.findById(id).orElse(null);
        if (thread == null) {
            throw new EntityNotFoundException("Thread not found");
        }
        
        return forumVoteMapper.toDTOs(forumVoteRepository.findByThread(thread));
    }
    
    @Transactional(readOnly = true)
    public ForumVoteDTO findById(Long id) throws EntityNotFoundException
    {
        ForumVote vote = forumVoteRepository.findById(id).orElse(null);
        if (vote == null) {
            throw new EntityNotFoundException("Vote not found");
        }
        
        return forumVoteMapper.toDTO(vote);
    }
    
    /**
     * Add a vote to a forum thread.
     *
     * @param id         ID of the forum thread.
     * @param likeValue Value of the vote. 1 for like, -1 for dislike.
     * @return {@link ForumThreadDTO}.
     * @throws EntityNotFoundException if forum thread with given ID is not found.
     * @throws EntityExistsException   if user already voted.
     */
    @Transactional
    public ForumThreadDTO voteThread(Long id, int likeValue) throws EntityNotFoundException, EntityExistsException {
        ForumThread thread = forumThreadRepository.findByIdWithMessages(id).orElse(null);
        if (thread == null){
            throw new EntityNotFoundException("Thread not found");
        }

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new EntityNotFoundException("User not found"));

        ForumVote existingVote = forumVoteRepository.findByUserAndThread(user, thread);
        if (existingVote != null){
            throw new EntityExistsException("User already voted");
        }

        VoteType voteType = likeValue > 0 ? VoteType.LIKE : VoteType.DISLIKE;
        ForumVote newVote = new ForumVote(user, thread, voteType);
        forumVoteRepository.save(newVote);
        
        // Reload thread with updated data
        ForumThread updatedThread = forumThreadRepository.findByIdWithMessages(id).orElse(thread);
        return forumThreadMapper.toDTO(updatedThread);
    }

    /**
     * Delete a vote from a forum thread.
     *
     * @param id ID of the forum thread.
     * @throws EntityNotFoundException if forum thread with given ID is not found.
     * @throws EntityNotFoundException if forum vote does not exist.
     */
    @Transactional
    public void unvoteThread(Long id) {
        ForumThread thread = forumThreadRepository.findById(id).orElse(null);
        if (thread == null){
            throw new EntityNotFoundException("Thread not found");
        }

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new EntityNotFoundException("User not found"));

        ForumVote vote = forumVoteRepository.findByUserAndThread(user, thread);
        if (vote == null){
            throw new EntityNotFoundException("Vote not found");
        }
        forumVoteRepository.delete(vote);
    }
}