package fr.airsen.api.controller;

import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.service.ForumMessageService;
import fr.airsen.api.service.ForumThreadService;
import fr.airsen.api.service.ForumVoteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Controller for forum threads.
 */
@RestController
@RequestMapping("/forum/threads")
@Tag(name = "Forum Thread", description = "Forum Thread endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ForumThreadController {

    @Autowired
    private ForumThreadService forumThreadService;

    @Autowired
    private ForumMessageService forumMessageService;

    @Autowired
    private ForumVoteService forumVoteService;

    /**
     * Get all forum threads.
     * @return List of all threads as a {@link List<ForumThreadDTO>}.
     */
    @GetMapping
    public List<ForumThreadDTO> getAllForumThreads(){
        return forumThreadService.findAll();
    }

    /**
     * Create a new forum thread.
     * @param forumThread the thread to create.
     * @return The created thread as a {@link ForumThreadDTO}.
     */
    @PostMapping
    public ForumThreadDTO createForumThread(@Valid @RequestBody ForumThread forumThread, BindingResult result){
        return forumThreadService.createThread(forumThread, result);
    }

    /**
     * Get a forum thread by its ID.
     * @param id Id of the thread.
     * @return The thread with the given ID as a {@link ForumThreadDTO}.
     */
    @GetMapping("/{id}")
    public ForumThreadDTO getForumThread(@PathVariable Long id){
        return forumThreadService.findById(id);
    }

    /**
     * Get all messages of a thread.
     * @param id Id of the thread.
     * @return List of messages of the thread as a {@link List<ForumThreadDTO>}.
     */
    @GetMapping("/{id}/messages")
    public List<ForumMessageDTO> getMessagesByThread(@PathVariable Long id){
        return forumMessageService.getMessagesByThread(id);
    }

    /**
     * Add a message to a thread
     * @param id Id of the thread to add the message to.
     * @param forumMessage the message to add.
     * @return The thread with the added message as a {@link ForumThreadDTO}.
     */
    @PostMapping("/{id}/messages")
    public ForumMessageDTO addMessageToThread(@PathVariable Long id, @RequestBody ForumMessage forumMessage, BindingResult result){
        return forumMessageService.addMessageToThread(id, forumMessage, result);
    }

    /**
     * Update a thread.
     * @param id Id of the thread to update.
     * @param forumThread the new content of the thread.
     * @return The updated thread as a {@link ForumThreadDTO}.
     */
    @PutMapping("/{id}")
    public ForumThreadDTO updateForumThread(@PathVariable Long id, @RequestBody ForumThread forumThread, BindingResult result){
        return forumThreadService.updateThread(id, forumThread, result);
    }

    /**
     * Delete a thread.
     * @param id Id of the thread to delete.
     * @return List of threads as a {@link List<ForumThreadDTO>}.
     */
    @DeleteMapping("/{id}")
    public List<ForumThreadDTO> deleteForumThread(@PathVariable Long id){
        return forumThreadService.deleteThread(id);
    }

    /**
     * Add a vote to a thread
     * @param id the Id of the thread to vote for.
     * @return The thread with the added vote as a {@link ForumThreadDTO}.
     */
    @PostMapping("/{id}/vote")
    public ForumThreadDTO voteThread(@PathVariable Long id, @RequestParam int likeValue){
        return forumVoteService.voteThread(id, likeValue);
    }

    /**
     * Remove a vote from a thread.
     * @param id the Id of the thread to unvote.
     */
    @DeleteMapping("/{id}/vote")
    public void unvoteThread(@PathVariable Long id){
        forumVoteService.unvoteThread(id);
    }

}
