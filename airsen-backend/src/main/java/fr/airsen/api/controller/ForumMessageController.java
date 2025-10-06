package fr.airsen.api.controller;

import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.dto.request.ForumMessageUpdateRequest;
import fr.airsen.api.service.ForumMessageService;
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
 * REST Controller for managing forum messages.
 * 
 * This controller provides endpoints for forum message management including
 * retrieval, updates, deletion, and voting operations.
 * All operations follow REST conventions and require appropriate authentication.
 */
@RestController
@RequestMapping("/forum/messages")
@Tag(name = "Forum Messages", description = "Forum message management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ForumMessageController {
    
    @Autowired
    private ForumMessageService forumMessageService;
    
    /**
     * GET /forum/messages - List all forum messages.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @return paginated list of forum messages
     */
    @GetMapping
    @Operation(summary = "List forum messages", 
              description = "Retrieve paginated list of all forum messages ordered by creation date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<ForumMessageDTO>> getAllMessages(){
        
        List<ForumMessageDTO> messages = forumMessageService.findAll();
        return ResponseEntity.ok(messages);
    }
    
    /**
     * GET /forum/messages/{id} - Get message by ID.
     * 
     * @param id message identifier
     * @return forum message details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get message by ID", 
              description = "Retrieve detailed information about a specific forum message")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Message not found", content = @Content)
    })
    public ResponseEntity<ForumMessageDTO> getMessage(
            @Parameter(description = "Message identifier") @PathVariable Long id){
        
        ForumMessageDTO message = forumMessageService.findById(id);
        return ResponseEntity.ok(message);
    }
    
    /**
     * PUT /forum/messages/{id} - Update message.
     *
     * @param id message identifier
     * @param request updated message content
     * @return updated message
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update forum message",
        description = "Update an existing forum message content. " +
                     "Only the message author or an admin can update a message."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data - validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Only author or admin can update", content = @Content),
        @ApiResponse(responseCode = "404", description = "Message not found", content = @Content)
    })
    public ResponseEntity<ForumMessageDTO> updateMessage(
            @Parameter(description = "Message identifier") @PathVariable Long id,
            @Valid @RequestBody ForumMessageUpdateRequest request) {

        ForumMessageDTO updatedMessage = forumMessageService.updateMessage(id, request);
        return ResponseEntity.ok(updatedMessage);
    }
    
    /**
     * DELETE /forum/messages/{id} - Delete message.
     * 
     * @param id message identifier
     * @return confirmation response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete forum message", 
              description = "Delete a forum message")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Message deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Only author or admin can delete", content = @Content),
        @ApiResponse(responseCode = "404", description = "Message not found", content = @Content)
    })
    public ResponseEntity<Void> deleteMessage(
            @Parameter(description = "Message identifier") @PathVariable Long id){
        
        forumMessageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }
    
}