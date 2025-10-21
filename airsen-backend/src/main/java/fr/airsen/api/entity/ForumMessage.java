package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * This entity represents a message in the forum. It is linked to a {@link ForumThread } entity.
 */
@Entity
@Table(name = "forum_messages", indexes = {
    @Index(name = "idx_message_author_deleted", columnList = "author_deleted")
})
public class ForumMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, length = 10)
    private long id;

    /**
     * The author of this forum message.
     */
    @ManyToOne
    @JoinColumn(name = "author_id", nullable = true)
    private User author;

    /**
     * Flag indicating whether the original author has been deleted.
     */
    @Column(name = "author_deleted", nullable = false)
    private Boolean authorDeleted = false;

    /**
     * Preserved display name of the original author (GDPR author preservation).
     */
    @Column(name = "author_name", length = 200)
    @Size(max = 200, message = "Author name cannot exceed 200 characters")
    private String authorName;

    @ManyToOne
    @JoinColumn(name = "thread_id")
    private ForumThread thread;

    @Column(name = "content", nullable = false, length = 65535)
    @Size(min = 1, message = "Content must not be empty")
    private String content;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    public ForumMessage() {
    }

    public ForumMessage(User author, ForumThread thread, String content, LocalDateTime createdDate) {
        this.author = author;
        this.authorDeleted = false;  // New messages always have active authors
        this.authorName = null;  // Only populated when author is deleted
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

    public Boolean getAuthorDeleted() {
        return authorDeleted;
    }

    public void setAuthorDeleted(Boolean authorDeleted) {
        this.authorDeleted = authorDeleted;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    // ==================== GDPR Author Preservation Business Methods ====================

    /**
     * Gets the display name of the message author for UI rendering.
     *
     * @return Display name of the message author (never null)
     */
    public String getAuthorDisplayName() {
        if (this.author != null) {
            // Active author - use current display name
            return this.author.getDisplayName();
        } else if (this.authorName != null) {
            // Deleted author - use preserved name
            return this.authorName;
        } else {
            // Data integrity issue - should never happen in production
            return "Unknown User";
        }
    }

    /**
     * Checks if this message's author has a clickable profile link.
     *
     * @return true if author profile link should be displayed, false otherwise
     */
    public boolean hasAuthorProfileLink() {
        if (this.author == null) {
            return false;  // Deleted authors have no profile link
        }
        return this.author.hasProfileLink();
    }

    /**
     * Gets the author ID for constructing profile link URLs.
     *
     * Returns the author's user ID if the author is active and has a profile link,
     * otherwise returns null. Use this method in conjunction with {@link #hasAuthorProfileLink()}
     * to safely generate profile URLs.
     *
     * Usage Pattern:
     * <{@code
     * if (message.hasAuthorProfileLink()) {
     *     Long authorId = message.getAuthorIdForLink();
     *     String profileUrl = "/users/" + authorId;
     *     // Render clickable link
     * } else {
     *     String authorName = message.getAuthorDisplayName();
     *     // Render plain text
     * }
     * }
     *
     * This method guarantees null safety. It returns null
     * for deleted authors or users with HIDDEN visibility, preventing broken links.
     *
     * @return Author user ID for profile link, or null if no link should be displayed
     */
    public Long getAuthorIdForLink() {
        if (!hasAuthorProfileLink()) {
            return null;  // No profile link allowed
        }
        return this.author.getId();
    }

    // ==================== End GDPR Author Preservation Methods ====================

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
