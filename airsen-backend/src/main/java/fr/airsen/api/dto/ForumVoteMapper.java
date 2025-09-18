package fr.airsen.api.DTO;

import fr.airsen.api.entity.ForumVote;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ForumVoteMapper {

    /**
     * Map entity to DTO
     * @param forumVote entity to map
     * @return mapped DTO
     */
    public ForumVoteDTO toDTO(ForumVote forumVote) {
        return new ForumVoteDTO(forumVote, true);
    }

    /**
     * Map DTO to entity
     * @param forumVoteDTO DTO to map
     * @return mapped entity
     */
    public ForumVote toEntity(ForumVoteDTO forumVoteDTO) {
        ForumVote forumVote = new ForumVote();
        forumVote.setId(forumVoteDTO.getId());
        forumVote.setVoteType(forumVoteDTO.getVoteType());
        return forumVote;
    }

    /**
     * Map list of entities to list of DTOs
     * @param forumCategories list of entities to map
     * @return mapped list of DTOs
     */
    public List<ForumVoteDTO> toDTOs(List<ForumVote> forumCategories) {
        List<ForumVoteDTO> forumVoteDTOList = new ArrayList<>();
        for (ForumVote forumVote : forumCategories) {
            forumVoteDTOList.add(toDTO(forumVote));
        }
        return forumVoteDTOList;
    }

    /**
     * Map list of DTOs to list of entities
     * @param forumVoteDTOList list of DTOs to map
     * @return mapped list of entities
     */
    public List<ForumVote> toEntities(List<ForumVoteDTO> forumVoteDTOList) {
        List<ForumVote> forumVoteList = new ArrayList<>();
        for (ForumVoteDTO forumVoteDTO : forumVoteDTOList) {
            forumVoteList.add(toEntity(forumVoteDTO));
        }
        return forumVoteList;
    }
}
