package fr.airsen.api.dto;

import fr.airsen.api.entity.ForumThread;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ForumThreadMapper {

    /**
     * Map entity to DTO
     * @param forumThread entity to map
     * @return mapped DTO
     */
    public ForumThreadDTO toDTO(ForumThread forumThread) {
        return new ForumThreadDTO(forumThread, true);
    }

    /**
     * Map DTO to entity
     * @param forumThreadDTO DTO to map
     * @return mapped entity
     */
    public ForumThread toEntity(ForumThreadDTO forumThreadDTO) {
        ForumThread forumThread = new ForumThread();
        forumThread.setId(forumThreadDTO.getId());
        forumThread.setTitle(forumThreadDTO.getTitle());
        forumThread.setContent(forumThreadDTO.getContent());
        forumThread.setCreatedDate(forumThreadDTO.getCreatedDate());
        forumThread.setLastMessageDate(forumThreadDTO.getLastMessageDate());
        forumThread.setViewCount(forumThreadDTO.getViewCount());
        forumThread.setPinned(forumThreadDTO.isPinned());
        forumThread.setClosed(forumThreadDTO.isClosed());
        forumThread.setLikeCount(forumThreadDTO.getLikeCount());
        return forumThread;
    }

    /**
     * Map list of entities to list of DTOs
     * @param forumCategories list of entities to map
     * @return mapped list of DTOs
     */
    public List<ForumThreadDTO> toDTOs(List<ForumThread> forumCategories) {
        List<ForumThreadDTO> forumThreadDTOList = new ArrayList<>();
        for (ForumThread forumThread : forumCategories) {
            forumThreadDTOList.add(toDTO(forumThread));
        }
        return forumThreadDTOList;
    }

    /**
     * Map list of DTOs to list of entities
     * @param forumThreadDTOList list of DTOs to map
     * @return mapped list of entities
     */
    public List<ForumThread> toEntities(List<ForumThreadDTO> forumThreadDTOList) {
        List<ForumThread> forumThreadList = new ArrayList<>();
        for (ForumThreadDTO forumThreadDTO : forumThreadDTOList) {
            forumThreadList.add(toEntity(forumThreadDTO));
        }
        return forumThreadList;
    }
}
