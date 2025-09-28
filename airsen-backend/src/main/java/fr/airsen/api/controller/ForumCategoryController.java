package fr.airsen.api.controller;

import fr.airsen.api.dto.ForumCategoryDTO;
import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.service.ForumCategoryService;
import fr.airsen.api.service.ForumThreadService;
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
 * REST Controller for managing forum categories.
 * 
 * This controller provides endpoints for forum category management including
 * listing, creation, updates, deletion, and retrieving associated threads.
 * All operations follow REST conventions and require appropriate authentication.
 */
@RestController
@RequestMapping("/forum/categories")
@Tag(name = "Forum Categories", description = "Forum category management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ForumCategoryController {

    @Autowired
    private ForumCategoryService service;

    @Autowired
    ForumThreadService forumThreadService;

    /**
     * GET /forum/categories - List all forum categories.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @return paginated list of forum categories
     */
    @GetMapping
    @Operation(summary = "List forum categories", 
              description = "Retrieve paginated list of all forum categories with basic information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<ForumCategoryDTO>> listCategories() {
        
        List<ForumCategoryDTO> categories = service.findAll();
        return ResponseEntity.ok(categories);
    }

    /**
     * GET /forum/categories/{id} - Get category by ID.
     * 
     * @param id category identifier
     * @return forum category details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", 
              description = "Retrieve detailed information about a specific forum category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<ForumCategoryDTO> getCategory(
            @Parameter(description = "Category identifier") @PathVariable Long id) {
        
        ForumCategoryDTO category = service.findById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * GET /forum/categories/{id}/threads - Get threads by category.
     * 
     * @param id category identifier
     * @param page page number (0-based)
     * @param size page size
     * @return paginated list of threads in the category
     */
    @GetMapping("/{id}/threads")
    @Operation(summary = "Get threads by category", 
              description = "Retrieve paginated list of threads in a specific forum category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Threads retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<List<ForumThreadDTO>> listThreadsByCategory(
            @Parameter(description = "Category identifier") @PathVariable Long id) {
        
        List<ForumThreadDTO> threads = forumThreadService.findByCategory(id);
        return ResponseEntity.ok(threads);
    }

    /**
     * POST /forum/categories/{id}/threads - Create new thread in category.
     * 
     * @param id category identifier
     * @param forumThread thread data
     * @return created thread
     */
    @PostMapping("/{id}/threads")
    @Operation(summary = "Create thread in category", 
              description = "Create a new forum thread within a specific category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Thread created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid thread data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<List<ForumThreadDTO>> addThread(
            @Parameter(description = "Category identifier") @PathVariable Long id, 
            @Valid @RequestBody ForumThread forumThread, BindingResult result) {
        
        List<ForumThreadDTO> threads = forumThreadService.addThreadToCategory(id, forumThread, result);
        return ResponseEntity.status(HttpStatus.CREATED).body(threads);
    }

    /**
     * POST /forum/categories - Create new category.
     * 
     * @param forumCategory category data
     * @return created category
     */
    @PostMapping
    @Operation(summary = "Create forum category", 
              description = "Create a new forum category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid category data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required", content = @Content)
    })
    public ResponseEntity<List<ForumCategoryDTO>> addCategory(
            @Valid @RequestBody ForumCategory forumCategory, BindingResult result) {
        
        List<ForumCategoryDTO> categories = service.addForumCategory(forumCategory, result);
        return ResponseEntity.status(HttpStatus.CREATED).body(categories);
    }

    /**
     * PUT /forum/categories/{id} - Update category.
     * 
     * @param id category identifier
     * @param forumCategory updated category data
     * @return updated category
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update forum category", 
              description = "Update an existing forum category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid category data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<List<ForumCategoryDTO>> editCategory(
            @Parameter(description = "Category identifier") @PathVariable Long id, 
            @Valid @RequestBody ForumCategory forumCategory, BindingResult result) {
        
        List<ForumCategoryDTO> categories = service.editForumCategory(id, forumCategory, result);
        return ResponseEntity.ok(categories);
    }

    /**
     * DELETE /forum/categories/{id} - Delete category.
     * 
     * @param id category identifier
     * @return confirmation response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete forum category", 
              description = "Delete a forum category and all associated threads")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<List<ForumCategoryDTO>> deleteCategory(
            @Parameter(description = "Category identifier") @PathVariable Long id) {
        
        List<ForumCategoryDTO> categories = service.deleteForumCategory(id);
        return ResponseEntity.ok(categories);
    }

}