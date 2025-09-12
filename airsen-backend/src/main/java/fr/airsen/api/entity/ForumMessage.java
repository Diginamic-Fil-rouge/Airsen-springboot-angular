package fr.airsen.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class ForumMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private User author;

    @ManyToOne
    private ForumThread thread;

    private String content;

    private LocalDateTime createdDate;

    public ForumMessage() {
    }

    public ForumMessage(User author, ForumThread thread, String content, LocalDateTime createdDate) {
        this.author = author;
        this.thread = thread;
        this.content = content;
        this.createdDate = createdDate;
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
