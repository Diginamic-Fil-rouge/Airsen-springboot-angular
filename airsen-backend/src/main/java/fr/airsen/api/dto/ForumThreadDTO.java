package fr.airsen.api.dto;

import fr.airsen.api.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * DTO for a forum thread.
 */
@Schema(description = "Forum thread with author and category information")
public class ForumThreadDTO {
    @Schema(description = "Thread ID", example = "1")
    private long id;

    @Schema(description = "Thread author (lightweight)")
    private ForumAuthorDTO author;

    @Schema(description = "Thread category")
    private ForumCategoryDTO category;

    @Schema(description = "Thread messages (only included when withEntities=true)")
    private List<ForumMessageDTO> messages;

    @Schema(description = "Thread votes (only included when withEntities=true)")
    private List<ForumVoteDTO> votes;

    @Schema(description = "Thread title", example = "Air Quality Discussion")
    private String title;

    @Schema(description = "Thread content")
    private String content;

    @Schema(description = "Creation date", example = "2025-01-04T10:00:00")
    private LocalDateTime createdDate;

    @Schema(description = "Last message date", example = "2025-01-05T14:30:00")
    private LocalDateTime lastMessageDate;

    @Schema(description = "View count", example = "42")
    private Integer viewCount;

    @Schema(description = "Whether thread is pinned", example = "false")
    private boolean pinned;

    @Schema(description = "Whether thread is closed", example = "false")
    private boolean closed;

    @Schema(description = "Like count", example = "15")
    private Integer likeCount;

    @Schema(description = "Number of messages in thread", example = "8")
    private Integer messageCount;

    public ForumThreadDTO() {
    }

    public ForumThreadDTO(ForumThread forumThread, boolean withEntities) {
        this.id = forumThread.getId();
        this.author = new ForumAuthorDTO(forumThread.getAuthor());
        this.category = new ForumCategoryDTO(forumThread.getCategory(), false);
        this.title = forumThread.getTitle();
        this.content = forumThread.getContent();
        this.createdDate = forumThread.getCreatedDate();
        this.lastMessageDate = forumThread.getLastMessageDate();
        this.viewCount = forumThread.getViewCount();
        this.pinned = forumThread.isPinned();
        this.closed = forumThread.isClosed();
        this.likeCount = forumThread.getLikeCount();
        this.messageCount = (forumThread.getMessages() != null) ? forumThread.getMessages().size() : 0;

        if (withEntities) {
            this.messages = new ArrayList<>();
            this.votes = new ArrayList<>();
            if (forumThread.getMessages() != null) {
                for (ForumMessage message : forumThread.getMessages()) {
                    this.messages.add(new ForumMessageDTO(message, false));
                }
            }
            if (forumThread.getVotes() != null) {
                for (ForumVote vote : forumThread.getVotes()) {
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

    public ForumAuthorDTO getAuthor() {
        return author;
    }

    public void setAuthor(ForumAuthorDTO author) {
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

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
}
