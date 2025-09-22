package fr.airsen.api.mapper;

import fr.airsen.api.dto.ForumCategoryDTO;
import fr.airsen.api.entity.ForumCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to map ForumCategory Entity to ForumCategoryDTO and vice versa
 */
@Component
public class ForumCategoryMapper {

    /**
     * Map entity to DTO
     * @param forumCategory entity to map
     * @return mapped DTO
     */
    public ForumCategoryDTO toDTO(ForumCategory forumCategory) {
        return new ForumCategoryDTO(forumCategory, true);
    }

    /**
     * Map DTO to entity
     * @param forumCategoryDTO DTO to map
     * @return mapped entity
     */
    public ForumCategory toEntity(ForumCategoryDTO forumCategoryDTO) {
        ForumCategory forumCategory = new ForumCategory();
        forumCategory.setId(forumCategoryDTO.getId());
        forumCategory.setName(forumCategoryDTO.getName());
        forumCategory.setDescription(forumCategoryDTO.getDescription());
        forumCategory.setColor(forumCategoryDTO.getColor());
        return forumCategory;
    }

    /**
     * Map list of entities to list of DTOs
     * @param forumCategories list of entities to map
     * @return mapped list of DTOs
     */
    public List<ForumCategoryDTO> toDTOs(List<ForumCategory> forumCategories) {
        List<ForumCategoryDTO> forumCategoryDTOList = new ArrayList<>();
        for (ForumCategory forumCategory : forumCategories) {
            forumCategoryDTOList.add(toDTO(forumCategory));
        }
        return forumCategoryDTOList;
    }

    /**
     * Map list of DTOs to list of entities
     * @param forumCategoryDTOList list of DTOs to map
     * @return mapped list of entities
     */
    public List<ForumCategory> toEntities(List<ForumCategoryDTO> forumCategoryDTOList) {
        List<ForumCategory> forumCategoryList = new ArrayList<>();
        for (ForumCategoryDTO forumCategoryDTO : forumCategoryDTOList) {
            forumCategoryList.add(toEntity(forumCategoryDTO));
        }
        return forumCategoryList;
    }
}