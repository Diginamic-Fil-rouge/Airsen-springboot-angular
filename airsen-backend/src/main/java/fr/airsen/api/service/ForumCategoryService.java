package fr.airsen.api.service;

import fr.airsen.api.DTO.ForumCategoryDTO;
import fr.airsen.api.DTO.ForumCategoryMapper;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.repository.ForumCategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Service for handling forum categories.
 */
@Service
public class ForumCategoryService {

    @Autowired
    private ForumCategoryRepository forumCategoryRepository;

    @Autowired
    private ForumCategoryMapper mapper;

    /**
     * Get all forum categories.
     *
     * @return List of forum categories.
     */
    public List<ForumCategoryDTO> findAll() {
        return mapper.toDTOs(forumCategoryRepository.findAll());
    }

    /**
     * Get forum category by id.
     *
     * @param id Id of the forum category.
     * @return Forum category with the specified id.
     * @throws EntityNotFoundException If there is no forum category with the specified id.
     */
    public ForumCategoryDTO findById(long id) throws EntityNotFoundException {
        ForumCategory category = forumCategoryRepository.findById(id);
        if (category == null)
        {
            throw new EntityNotFoundException("Category not found");
        }
        return mapper.toDTO(category);
    }

    /**
     * Add new forum category.
     *
     * @param forumCategory Forum category to be added.
     * @return List of all forum categories.
     * @throws EntityExistsException If forum category with the same id already exists.
     */
    public List<ForumCategoryDTO> addForumCategory(@RequestBody ForumCategory forumCategory, BindingResult result) throws EntityExistsException, IllegalArgumentException {
        if (result.hasErrors()){
            throw new IllegalArgumentException("Invalid category : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumCategory entity = forumCategoryRepository.findByName(forumCategory.getName());
        if (entity != null){
            throw new EntityExistsException("Failed to add category - Category with same name already exists");
        }
        forumCategoryRepository.save(forumCategory);
        return mapper.toDTOs(forumCategoryRepository.findAll());
    }

    /**
     * Edit forum category.
     *
     * @param id             Id of the forum category to be edited.
     * @param forumCategory Forum category with new values.
     * @return List of all forum categories.
     * @throws EntityExistsException If forum category with the same id already exists.
     */
    public List<ForumCategoryDTO> editForumCategory(long id, @RequestBody ForumCategory forumCategory, BindingResult result) throws EntityExistsException, EntityNotFoundException, IllegalArgumentException {
        if (result.hasErrors()){
            throw new IllegalArgumentException("Invalid category : " + result.getAllErrors().get(0).getDefaultMessage());
        }
        ForumCategory entityExists = forumCategoryRepository.findByName(forumCategory.getName());
        if (entityExists != null && entityExists.getId() != id){
            throw new EntityExistsException("Failed to update category - Category with same name already exists");
        }
        else if (entityExists == null){
            throw new EntityNotFoundException("Failed to update category - Category not found");
        }
        forumCategoryRepository.save(forumCategory);
        return mapper.toDTOs(forumCategoryRepository.findAll());
    }

    /**
     * Delete forum category.
     *
     * @param id Id of the forum category to be deleted.
     * @return List of all forum categories.
     * @throws EntityNotFoundException If there is no forum category with the specified id.
     */
    public List<ForumCategoryDTO> deleteForumCategory(long id) throws EntityNotFoundException {
        ForumCategory entityExists = forumCategoryRepository.findById(id);
        if (entityExists == null){
            throw new EntityNotFoundException("Failed to delete category - Category not found");
        }
        forumCategoryRepository.deleteById(id);
        return mapper.toDTOs(forumCategoryRepository.findAll());
    }
}