package fr.airsen.api.controller;

import fr.airsen.api.DTO.ForumMessageDTO;
import fr.airsen.api.DTO.ForumThreadDTO;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.service.ForumMessageService;
import fr.airsen.api.service.ForumThreadService;
import fr.airsen.api.service.ForumVoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for forum threads.
 */
@RestController
@RequestMapping("/forum/threads")
@Tag(name = "Forum Thread", description = "Forum Thread endpoints")
public class ForumThreadController {

    @Autowired
    private ForumThreadService forumThreadService;

    @Autowired
    private ForumMessageService forumMessageService;

    @Autowired
    private ForumVoteService forumVoteService;

    /**
     * Get a forum thread by its ID.
     * @param id Id of the thread.
     * @return The thread with the given ID as a {@link ForumThreadDTO}.
     */
    @GetMapping("/{id}")
    public ForumThreadDTO getForumThread(long id){
        return forumThreadService.findById(id);
    }

    /**
     * Get all messages of a thread.
     * @param id Id of the thread.
     * @return List of messages of the thread as a {@link List<ForumThreadDTO>}.
     */
    @GetMapping("/{id}/messages")
    public List<ForumMessageDTO> getMessagesByThread(long id){
        return forumMessageService.getMessagesByThread(id);
    }

    /**
     * Add a message to a thread
     * @param id Id of the thread to add the message to.
     * @param forumMessage the message to add.
     * @return The thread with the added message as a {@link ForumThreadDTO}.
     */
    @PostMapping("/{id}/messages")
    public ForumMessageDTO addMessageToThread(long id, @RequestBody ForumMessage forumMessage, BindingResult result){
        return forumMessageService.addMessageToThread(id, forumMessage, result);
    }

    /**
     * Update a thread.
     * @param id Id of the thread to update.
     * @param forumThread the new content of the thread.
     * @return The updated thread as a {@link ForumThreadDTO}.
     */
    @PutMapping("/{id}")
    public ForumThreadDTO updateForumThread(long id, @RequestBody ForumThread forumThread, BindingResult result){
        return forumThreadService.updateThread(id, forumThread, result);
    }

    /**
     * Delete a thread.
     * @param id Id of the thread to delete.
     * @return List of threads as a {@link List<ForumThreadDTO>}.
     */
    @DeleteMapping("/{id}")
    public List<ForumThreadDTO> deleteForumThread(long id){
        return forumThreadService.deleteThread(id);
    }

//    /**
//     * Add a vote to a thread
//     * @param id the Id of the thread to vote for.
//     * @return The thread with the added vote as a {@link ForumThreadDTO}.
//     */
//    @PostMapping("/{id}/vote")
//    public ForumThreadDTO voteThread(long id, int likeValue){
//        return forumVoteService.voteThread(id, likeValue);
//    }
//
//    /**
//     * Remove a vote from a thread.
//     * @param id the Id of the thread to unvote.
//     */
//    @DeleteMapping("/{id}/vote")
//    public void unvoteThread(long id){
//        forumVoteService.unvoteThread(id);
//    }

}
