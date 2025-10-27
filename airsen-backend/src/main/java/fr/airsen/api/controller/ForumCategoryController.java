package fr.airsen.api.controller;

import fr.airsen.api.dto.ForumCategoryDTO;
import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.dto.request.ForumCategoryCreateRequest;
import fr.airsen.api.dto.request.ForumCategoryUpdateRequest;
import fr.airsen.api.dto.request.ForumThreadCreateRequest;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
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

        threads.sort(Comparator.comparing(ForumThreadDTO::getLastMessageDate).reversed());

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
            @Valid @RequestBody ForumThreadCreateRequest request) {

        List<ForumThreadDTO> threads = forumThreadService.addThreadToCategory(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(threads);
    }

    /**
     * POST /forum/categories - Create new category.
     *
     * @param request category creation request with minimal fields
     * @return created category
     */
    @PostMapping
    @Operation(summary = "Create forum category",
              description = "Create a new forum category with minimal fields (name, description, color)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid category data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required", content = @Content)
    })
    public ResponseEntity<List<ForumCategoryDTO>> addCategory(
            @Valid @RequestBody ForumCategoryCreateRequest request) {

        List<ForumCategoryDTO> categories = service.addForumCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(categories);
    }

    /**
     * PUT /forum/categories/{id} - Update category.
     *
     * @param id category identifier
     * @param request updated category data (all fields optional for partial update)
     * @return updated category
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update forum category",
              description = "Update an existing forum category. All fields are optional - only provided fields will be updated.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid category data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<List<ForumCategoryDTO>> editCategory(
            @Parameter(description = "Category identifier") @PathVariable Long id,
            @Valid @RequestBody ForumCategoryUpdateRequest request) {

        List<ForumCategoryDTO> categories = service.editForumCategory(id, request);
        return ResponseEntity.ok(categories);
    }

    /**
     * DELETE /forum/categories/{id} - Delete category.
     *
     * @param id category identifier
     * @return no content response (204)
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
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Category identifier") @PathVariable Long id) {

        service.deleteForumCategory(id);
        return ResponseEntity.noContent().build();
    }

}