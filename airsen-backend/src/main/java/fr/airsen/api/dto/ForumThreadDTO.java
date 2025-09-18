package fr.airsen.api.DTO;

import fr.airsen.api.entity.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * DTO for a forum thread.
 */
public class ForumThreadDTO {
    private long id;

    private User author;

    private ForumCategoryDTO category;

    private List<ForumMessageDTO> messages;

    private List<ForumVoteDTO> votes;

    private String title;

    private String content;

    private LocalDateTime createdDate;

    private LocalDateTime lastMessageDate;

    private Integer viewCount;

    private boolean pinned;

    private boolean closed;

    private Integer likeCount;

    public ForumThreadDTO() {
    }

    public ForumThreadDTO(ForumThread forumThread, boolean withEntities){
        this.id = forumThread.getId();
        this.author = forumThread.getAuthor();
        // TODO after UserDTO implementation
//        this.author = new UserDTO(forumThread.getAuthor());
        this.category = new ForumCategoryDTO(forumThread.getCategory(), false);
        this.title = forumThread.getTitle();
        this.content = forumThread.getContent();
        this.createdDate = forumThread.getCreatedDate();
        this.lastMessageDate = forumThread.getLastMessageDate();
        this.viewCount = forumThread.getViewCount();
        this.pinned = forumThread.isPinned();
        this.closed = forumThread.isClosed();
        this.likeCount = forumThread.getLikeCount();
        if(withEntities){
            this.messages = new ArrayList<>();
            this.votes = new ArrayList<>();
            if (forumThread.getMessages() != null){
                for (ForumMessage message : forumThread.getMessages()) {
                    this.messages.add(new ForumMessageDTO(message, false));
                }
            }
            if (forumThread.getVotes() != null){
                for (ForumVote vote : forumThread.getVotes()){
                    this.votes.add(new ForumVoteDTO(vote, false));
                }
            }
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public ForumCategoryDTO getCategory() {
        return category;
    }

    public void setCategory(ForumCategoryDTO category) {
        this.category = category;
    }

    public List<ForumMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<ForumMessageDTO> messages) {
        this.messages = messages;
    }

    public List<ForumVoteDTO> getVotes() {
        return votes;
    }

    public void setVotes(List<ForumVoteDTO> votes) {
        this.votes = votes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(LocalDateTime lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }
}
