package fr.airsen.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class ForumThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private User author;

    @ManyToOne
    private ForumCategory category;

    private String title;

    private String content;

    private LocalDateTime createdDate;

    private LocalDateTime lastMessageDate;

    private Integer viewCount;

    private boolean pinned;

    private boolean closed;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
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
