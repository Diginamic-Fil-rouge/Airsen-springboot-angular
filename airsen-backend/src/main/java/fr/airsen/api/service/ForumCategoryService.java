package fr.airsen.api.service;

import fr.airsen.api.dto.ForumCategoryDTO;
import fr.airsen.api.dto.request.ForumCategoryCreateRequest;
import fr.airsen.api.dto.request.ForumCategoryUpdateRequest;
import fr.airsen.api.mapper.ForumCategoryMapper;
import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.repository.ForumCategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForumCategoryService {

    @Autowired
    private ForumCategoryRepository forumCategoryRepository;

    @Autowired
    private ForumCategoryMapper mapper;

    @Transactional
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
    @Transactional
    public ForumCategoryDTO findById(Long id) throws EntityNotFoundException {
        ForumCategory category = forumCategoryRepository.findById(id).orElse(null);
        if (category == null)
        {
            throw new EntityNotFoundException("Category not found");
        }
        return mapper.toDTO(category);
    }

    /**
     * Add new forum category.
     *
     * @param request Forum category creation request with minimal fields.
     * @return List of all forum categories.
     * @throws EntityExistsException If forum category with the same name already exists.
     */
    @Transactional
    public List<ForumCategoryDTO> addForumCategory(ForumCategoryCreateRequest request) throws EntityExistsException {
        ForumCategory existingCategory = forumCategoryRepository.findByName(request.getName());
        if (existingCategory != null){
            throw new EntityExistsException("Failed to add category - Category with same name already exists");
        }

        ForumCategory forumCategory = new ForumCategory();
        forumCategory.setName(request.getName());
        forumCategory.setDescription(request.getDescription());
        forumCategory.setColor(request.getColor());

        forumCategoryRepository.save(forumCategory);
        return mapper.toDTOs(forumCategoryRepository.findAll());
    }

    /**
     * Edit forum category.
     *
     * @param id      Id of the forum category to be edited.
     * @param request Forum category update request with optional fields.
     * @return List of all forum categories.
     * @throws EntityExistsException   If forum category with the same name already exists.
     * @throws EntityNotFoundException If category with given id does not exist.
     */
    @Transactional
    public List<ForumCategoryDTO> editForumCategory(Long id, ForumCategoryUpdateRequest request) throws EntityExistsException, EntityNotFoundException {
        ForumCategory categoryExists = forumCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Failed to update category - Category not found"));

        // Check if new name conflicts with existing category
        if (request.getName() != null) {
            ForumCategory entityWithSameName = forumCategoryRepository.findByName(request.getName());
            if (entityWithSameName != null && entityWithSameName.getId() != id){
                throw new EntityExistsException("Failed to update category - Category with same name already exists");
            }
            categoryExists.setName(request.getName());
        }

        // Update only non-null fields (partial update)
        if (request.getDescription() != null) {
            categoryExists.setDescription(request.getDescription());
        }

        if (request.getColor() != null) {
            categoryExists.setColor(request.getColor());
        }

        forumCategoryRepository.save(categoryExists);
        return mapper.toDTOs(forumCategoryRepository.findAll());
    }

    /**
     * Delete forum category.
     *
     * @param id Id of the forum category to be deleted.
     * @throws EntityNotFoundException If there is no forum category with the specified id.
     */
    @Transactional
    public void deleteForumCategory(Long id) throws EntityNotFoundException {
        ForumCategory entityExists = forumCategoryRepository.findById(id).orElse(null);
        if (entityExists == null){
            throw new EntityNotFoundException("Failed to delete category - Category not found");
        }
        forumCategoryRepository.deleteById(id);
    }
}