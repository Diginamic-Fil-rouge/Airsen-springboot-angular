package fr.airsen.api.controller;

import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.service.ForumMessageService;
import fr.airsen.api.service.ForumThreadService;
import fr.airsen.api.service.ForumVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing forum threads.
 * 
 * This controller provides endpoints for forum thread management including
 * listing, creation, updates, deletion, message management, and voting.
 * All operations follow REST conventions and require appropriate authentication.
 */
@RestController
@RequestMapping("/forum/threads")
@Tag(name = "Forum Threads", description = "Forum thread management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ForumThreadController {

    @Autowired
    private ForumThreadService forumThreadService;

    @Autowired
    private ForumMessageService forumMessageService;

    @Autowired
    private ForumVoteService forumVoteService;

    /**
     * GET /forum/threads - List all forum threads.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @return paginated list of forum threads
     */
    @GetMapping
    @Operation(summary = "List forum threads", 
              description = "Retrieve paginated list of all forum threads ordered by creation date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Threads retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<ForumThreadDTO>> getAllForumThreads(){
        
        List<ForumThreadDTO> threads = forumThreadService.findAll();
        return ResponseEntity.ok(threads);
    }

    /**
     * POST /forum/threads - Create new forum thread.
     * 
     * @param forumThread thread data
     * @return created thread
     */
    @PostMapping
    @Operation(summary = "Create forum thread", 
              description = "Create a new forum thread")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Thread created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid thread data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<ForumThreadDTO> createForumThread(
            @Valid @RequestBody ForumThread forumThread, BindingResult result){
        
        ForumThreadDTO createdThread = forumThreadService.createThread(forumThread, result);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdThread);
    }

    /**
     * GET /forum/threads/{id} - Get thread by ID.
     * 
     * @param id thread identifier
     * @return forum thread details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get thread by ID", 
              description = "Retrieve detailed information about a specific forum thread")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thread retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    public ResponseEntity<ForumThreadDTO> getForumThread(
            @Parameter(description = "Thread identifier") @PathVariable Long id){
        
        ForumThreadDTO thread = forumThreadService.findById(id);
        return ResponseEntity.ok(thread);
    }

    /**
     * GET /forum/threads/{id}/messages - Get messages by thread.
     * 
     * @param id thread identifier
     * @param page page number (0-based)
     * @param size page size
     * @return paginated list of messages in the thread
     */
    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages by thread", 
              description = "Retrieve paginated list of messages in a specific forum thread")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    public ResponseEntity<List<ForumMessageDTO>> getMessagesByThread(
            @Parameter(description = "Thread identifier") @PathVariable Long id){
        
        List<ForumMessageDTO> messages = forumMessageService.getMessagesByThread(id);
        return ResponseEntity.ok(messages);
    }

    /**
     * POST /forum/threads/{id}/messages - Create message in thread.
     * 
     * @param id thread identifier
     * @param forumMessage message data
     * @return created message
     */
    @PostMapping("/{id}/messages")
    @Operation(summary = "Create message in thread", 
              description = "Create a new message within a specific forum thread")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Message created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid message data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    public ResponseEntity<ForumMessageDTO> addMessageToThread(
            @Parameter(description = "Thread identifier") @PathVariable Long id, 
            @Valid @RequestBody ForumMessage forumMessage, BindingResult result){
        
        ForumMessageDTO createdMessage = forumMessageService.addMessageToThread(id, forumMessage, result);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMessage);
    }

    /**
     * PUT /forum/threads/{id} - Update thread.
     * 
     * @param id thread identifier
     * @param forumThread updated thread data
     * @return updated thread
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update forum thread", 
              description = "Update an existing forum thread")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thread updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid thread data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Only author or admin can update", content = @Content),
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    public ResponseEntity<ForumThreadDTO> updateForumThread(
            @Parameter(description = "Thread identifier") @PathVariable Long id, 
            @Valid @RequestBody ForumThread forumThread, BindingResult result){
        
        ForumThreadDTO updatedThread = forumThreadService.updateThread(id, forumThread, result);
        return ResponseEntity.ok(updatedThread);
    }

    /**
     * DELETE /forum/threads/{id} - Delete thread.
     * 
     * @param id thread identifier
     * @return confirmation response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete forum thread", 
              description = "Delete a forum thread and all associated messages")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Thread deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Only author or admin can delete", content = @Content),
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    public ResponseEntity<List<ForumThreadDTO>> deleteForumThread(
            @Parameter(description = "Thread identifier") @PathVariable Long id){
        
        List<ForumThreadDTO> threads = forumThreadService.deleteThread(id);
        return ResponseEntity.ok(threads);
    }

    /**
     * POST /forum/threads/{id}/vote - Vote on thread.
     * 
     * @param id thread identifier
     * @param voteRequest vote data (like value)
     * @return updated thread with vote count
     */
    @PostMapping("/{id}/vote")
    @Operation(summary = "Vote on thread", 
              description = "Cast a vote (like/dislike) on a forum thread")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vote recorded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid vote data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    public ResponseEntity<ForumThreadDTO> voteThread(
            @Parameter(description = "Thread identifier") @PathVariable Long id, 
            @Parameter(description = "Vote value: 1 for like, -1 for dislike")
            @RequestParam("likeValue") int likeValue){
        
        ForumThreadDTO updatedThread = forumVoteService.voteThread(id, likeValue);
        return ResponseEntity.ok(updatedThread);
    }

    /**
     * DELETE /forum/threads/{id}/vote - Remove vote from thread.
     * 
     * @param id thread identifier
     * @return confirmation response
     */
    @DeleteMapping("/{id}/vote")
    @Operation(summary = "Remove vote from thread", 
              description = "Remove user's vote from a forum thread")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Vote removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Thread or vote not found", content = @Content)
    })
    public ResponseEntity<Void> unvoteThread(
            @Parameter(description = "Thread identifier") @PathVariable Long id){
        
        forumVoteService.unvoteThread(id);
        return ResponseEntity.noContent().build();
    }

}
