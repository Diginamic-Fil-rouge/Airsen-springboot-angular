package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * This entity represents a message in the forum. It is linked to a {@link ForumThread } entity.
 */
@Entity
@Table(name = "forum_messages")
public class ForumMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, length = 10)
    private long id;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "thread_id", nullable = false)
    private ForumThread thread;

    @Column(name = "content", nullable = false, length = 65535)
    @Min(value = 1, message = "Content must not be empty")
    private String content;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    public ForumMessage() {
    }

    public ForumMessage(User author, ForumThread thread, String content, LocalDateTime createdDate) {
        this.author = author;
        this.thread = thread;
        this.content = content;
        this.createdDate = createdDate;
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

    public ForumThread getThread() {
        return thread;
    }

    public void setThread(ForumThread thread) {
        this.thread = thread;
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

    @Override
    public String toString() {
        return "ForumMessage{" +
                "id=" + id +
                ", author=" + author +
                ", thread=" + thread +
                ", content='" + content + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ForumMessage that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(author, that.author) && Objects.equals(thread, that.thread) && Objects.equals(content, that.content) && Objects.equals(createdDate, that.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, author, thread, content, createdDate);
    }
}
