package fr.airsen.api.mapper;


import fr.airsen.api.dto.ForumMessageDTO;
import fr.airsen.api.entity.ForumMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ForumMessageMapper {

    /**
     * Map entity to DTO
     * @param forumMessage entity to map
     * @return mapped DTO
     */
    public ForumMessageDTO toDTO(ForumMessage forumMessage) {
        return new ForumMessageDTO(forumMessage, true);
    }

    /**
     * Map DTO to entity
     * @param forumMessageDTO DTO to map
     * @return mapped entity
     */
    public ForumMessage toEntity(ForumMessageDTO forumMessageDTO) {
        ForumMessage forumMessage = new ForumMessage();
        forumMessage.setId(forumMessageDTO.getId());
        forumMessage.setContent(forumMessageDTO.getContent());
        forumMessage.setCreatedDate(forumMessageDTO.getCreatedDate());
        forumMessage.setAuthor(forumMessageDTO.getAuthor());
        return forumMessage;
    }

    /**
     * Map list of entities to list of DTOs
     * @param forumCategories list of entities to map
     * @return mapped list of DTOs
     */
    public List<ForumMessageDTO> toDTOs(List<ForumMessage> forumCategories) {
        List<ForumMessageDTO> forumMessageDTOList = new ArrayList<>();
        for (ForumMessage forumMessage : forumCategories) {
            forumMessageDTOList.add(toDTO(forumMessage));
        }
        return forumMessageDTOList;
    }

    /**
     * Map list of DTOs to list of entities
     * @param forumMessageDTOList list of DTOs to map
     * @return mapped list of entities
     */
    public List<ForumMessage> toEntities(List<ForumMessageDTO> forumMessageDTOList) {
        List<ForumMessage> forumMessageList = new ArrayList<>();
        for (ForumMessageDTO forumMessageDTO : forumMessageDTOList) {
            forumMessageList.add(toEntity(forumMessageDTO));
        }
        return forumMessageList;
    }
}
