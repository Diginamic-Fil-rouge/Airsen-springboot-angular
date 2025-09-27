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

    public List<ForumVoteDTO> findAllVoteByThread(long id) throws EntityNotFoundException
    {
        ForumThread thread = forumThreadRepository.findById(id).orElse(null);
        if (thread == null) {
            throw new EntityNotFoundException("Thread not found");
        }
        
        return forumVoteMapper.toDTOs(forumVoteRepository.findByThread(thread));
    }
    
    public ForumVoteDTO findById(long id) throws EntityNotFoundException
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
    public ForumThreadDTO voteThread(long id, int likeValue) throws EntityNotFoundException, EntityExistsException {
        ForumThread thread = forumThreadRepository.findById(id).orElse(null);
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
        return forumThreadMapper.toDTO(thread);
    }

    /**
     * Delete a vote from a forum thread.
     *
     * @param id ID of the forum thread.
     * @throws EntityNotFoundException if forum thread with given ID is not found.
     * @throws EntityNotFoundException if forum vote does not exist.
     */
    public void unvoteThread(long id) {
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