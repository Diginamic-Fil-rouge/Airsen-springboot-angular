package fr.airsen.api.controller;

import fr.airsen.api.dto.ForumCategoryDTO;
import fr.airsen.api.dto.ForumThreadDTO;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.service.ForumCategoryService;
import fr.airsen.api.service.ForumThreadService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * This class is the controller for the forum categories. It handles the HTTP requests related to forum categories.
 */
@RestController
@RequestMapping("/forum/categories")
@Tag(name = "Forum Category", description = "Forum Category endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ForumCategoryController {

    @Autowired
    private ForumCategoryService service;

    @Autowired
    ForumThreadService forumThreadService;

    /**
     * Endpoint to list all the forum categories.
     *
     * @return List of forum categories.
     */
    @GetMapping
    public List<ForumCategoryDTO> listCategories() {
        return service.findAll();
    }

    /**
     * Endpoint to list all the threads of a specific forum category.
     *
     * @param id Category ID.
     * @return List of forum threads.
     */
    /**
     * Endpoint to get a single forum category by ID.
     *
     * @param id Category ID.
     * @return Forum category.
     */
    @GetMapping("/{id}")
    public ForumCategoryDTO getCategory(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/threads")
    public List<ForumThreadDTO> listThreadsByCategory(@PathVariable Long id) {
        return forumThreadService.findByCategory(id);
    }

    /**
     * Endpoint to add a new thread to a forum category.
     *
     * @param id       Category ID.
     * @param forumThread Thread to be added.
     * @return List of all forum threads.
     */
    @PostMapping("/{id}/threads")
    public List<ForumThreadDTO> addThread(@PathVariable Long id, @RequestBody ForumThread forumThread, BindingResult result) {
        return forumThreadService.addThreadToCategory(id, forumThread, result);
    }

    /**
     * Endpoint to add a new forum category.
     *
     * @param forumCategory Category to be added.
     * @return List of all forum categories.
     */
    @PostMapping
    public List<ForumCategoryDTO> addCategory(@RequestBody ForumCategory forumCategory, BindingResult result) {
        return service.addForumCategory(forumCategory, result);
    }

    /**
     * Endpoint to edit a forum category.
     *
     * @param id             Category ID.
     * @param forumCategory Category with new values.
     * @return List of all forum categories.
     */
    @PutMapping("/edit/{id}")
    public List<ForumCategoryDTO> editCategory(@PathVariable Long id, @RequestBody ForumCategory forumCategory, BindingResult result) {
        return service.editForumCategory(id, forumCategory, result);
    }

    /**
     * Endpoint to delete a forum category.
     *
     * @param id Category ID.
     * @return List of all forum categories.
     */
    @DeleteMapping("/delete/{id}")
    public List<ForumCategoryDTO> deleteCategory(@PathVariable Long id) {
        return service.deleteForumCategory(id);
    }

}