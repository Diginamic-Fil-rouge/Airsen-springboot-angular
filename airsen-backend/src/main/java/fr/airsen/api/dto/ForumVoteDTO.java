package fr.airsen.api.dto;

import fr.airsen.api.entity.ForumVote;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.VoteType;
import jakarta.persistence.*;

public class ForumVoteDTO {

    private long id;

    private User user;

    private ForumThreadDTO thread;

    private VoteType voteType;

    public ForumVoteDTO() {

    }

    public ForumVoteDTO(ForumVote forumVote, boolean withEntities)
    {
        this.id = forumVote.getId();
        this.voteType = forumVote.getVoteType();
        if (withEntities){
            this.user = forumVote.getUser();
            // TODO after UserDTO implementation
//            this.user = new UserDTO(forumVote.getUser());
            this.thread = new ForumThreadDTO(forumVote.getThread(), false);
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ForumThreadDTO getThread() {
        return thread;
    }

    public void setThread(ForumThreadDTO thread) {
        this.thread = thread;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }
}
