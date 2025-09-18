package fr.airsen.api.controller;

import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.service.ForumMessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for forum messages.
 */
@RestController
@RequestMapping("/forum/messages")
@Tag(name = "Forum Messages", description = "Forum Messages endpoints")
public class ForumMessageController {
    
    @Autowired
    private ForumMessageService forumMessageService;
    
    /**
     * Get a forum message by its id.
     *
     * @param id id of the forum message
     * @return the forum message
     */
    @GetMapping("/{id}")
    public ForumMessageDTO getMessage(@PathVariable long id){
        return forumMessageService.findById(id);
    }
    
    /**
     * Update a forum message by its id.
     *
     * @param id id of the forum message
     * @param forumMessage updated forum message
     * @return the updated forum message
     */
    @PutMapping("/{id}")
    public ForumMessageDTO updateMessage(@PathVariable long id, @RequestBody ForumMessage forumMessage, BindingResult result){
        return forumMessageService.updateMessage(id, forumMessage, result);
    }
    
    /**
     * Delete a forum message by its id.
     *
     * @param id id of the forum message
     */
    @DeleteMapping("/{id}")
    public void deleteMessage(@PathVariable long id){
        forumMessageService.deleteMessage(id);
    }
    
}