package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * This entity represents a thread in the forum. It is linked to a {@link ForumCategory } entity.
 */
@Entity
@Table(name = "forum_thread")
public class ForumThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, length = 10)
    private long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ForumCategory category;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ForumMessage> messages;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ForumVote> votes;

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Column(name = "content", nullable = false, length = 65535)
    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must be less than 10000 characters")
    private String content;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_message_date")
    private LocalDateTime lastMessageDate;

    @Column(name = "view_count", length = 10)
    private Integer viewCount;

    private boolean pinned;

    private boolean closed;

    @Column(name = "like_count", length = 10)
    private Integer likeCount;

    public ForumThread() {
    }

    public ForumThread(User author, ForumCategory category, String title, String content, LocalDateTime createdDate, LocalDateTime lastMessageDate, Integer viewCount, boolean pinned, boolean closed, Integer likeCount) {
        this.author = author;
        this.category = category;
        this.title = title;
        this.content = content;
        this.createdDate = createdDate;
        this.lastMessageDate = lastMessageDate;
        this.viewCount = viewCount;
        this.pinned = pinned;
        this.closed = closed;
        this.likeCount = likeCount;
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

    public ForumCategory getCategory() {
        return category;
    }

    public void setCategory(ForumCategory category) {
        this.category = category;
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

    public int getVotesValue(){
        if (this.votes == null){
            return 0;
        }
        int votesValue = 0;

        for (ForumVote vote : this.votes){
            votesValue += vote.getVoteType().toInt();
        }

        return votesValue;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public List<ForumMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ForumMessage> messages) {
        this.messages = messages;
    }

    public List<ForumVote> getVotes() {
        return votes;
    }

    public void setVotes(List<ForumVote> votes) {
        this.votes = votes;
    }

    @Override
    public String toString() {
        return "ForumThread{" +
                "id=" + id +
                ", author=" + author +
                ", category=" + category +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", createdDate=" + createdDate +
                ", lastMessageDate=" + lastMessageDate +
                ", viewCount=" + viewCount +
                ", pinned=" + pinned +
                ", closed=" + closed +
                ", likeCount=" + likeCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ForumThread that)) return false;
        return Objects.equals(author, that.author) && Objects.equals(category, that.category) && Objects.equals(title, that.title) && Objects.equals(createdDate, that.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, category, title, createdDate);
    }
}
