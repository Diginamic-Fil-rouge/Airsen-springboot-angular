package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


@Entity
@Table(name = "forum_threads", indexes = {
    @Index(name = "idx_thread_author_deleted", columnList = "author_deleted")
})
public class ForumThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, length = 10)
    private long id;

    /**
     * The author of this forum thread.
     *
     * Business Rules:
     * - If author != null → active user, use author.getDisplayName() for display
     * - If author == null AND authorName != null → deleted user, use authorName for display
     * - If both null → data integrity issue (should never happen in production)
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "author_id", nullable = true)
    private User author;

    /**
     * Flag indicating whether the original author has been deleted.
     *
     * When true, the author field is null and authorName contains the preserved display name.
     * This flag allows efficient querying for threads with deleted authors without checking for
     * null author relationships.
     *
     * Database Performance: Indexed via idx_thread_author_deleted for fast
     * queries to find threads by deleted authors (e.g., "show me all my threads even if I'm deleted").
     */
    @Column(name = "author_deleted", nullable = false)
    private Boolean authorDeleted = false;

    /**
     * Preserved display name of the original author (GDPR author preservation).
     *
     * This field is null when the author is active (use author.getDisplayName() instead).
     * When a user is deleted, their display name is copied to this field before the author
     * relationship is set to null. This preserves discussion context while respecting the
     * user's right to erasure.
     *
     * Example Values: "Marie Dupont", "Jean Martin", "user@example.com"
     * (falls back to email if user had no first/last name)
     */
    @Column(name = "author_name", length = 200)
    @Size(max = 200, message = "Author name cannot exceed 200 characters")
    private String authorName;

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
        this.authorDeleted = false;  // New threads always have active authors
        this.authorName = null;  // Only populated when author is deleted
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
     * Gets the display name of the thread author for UI rendering.
     *
     * This method intelligently returns the author name based on deletion status:
     * - Active author (author != null): Returns author.getDisplayName()
     *                                     (user's full name or email)
     * - Deleted author (author == null): Returns the preserved authorName
     *                                      field (e.g., "Marie Dupont")
     * - Data integrity issue (both null): Returns "Unknown User" as
     *                                       fallback (should never happen in production)
     *
     * Usage in UI: Use this method anywhere you need to display the
     * thread author's name (thread list, thread detail page, user profile, etc.). Do NOT
     * directly call author.getDisplayName() as it will throw NullPointerException for
     * deleted authors.
     *
     * @return Display name of the thread author (never null)
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
     * Checks if this thread's author has a clickable profile link.
     *
     * Returns true only if the author is an active user (author != null) and their
     * profile visibility settings allow showing a profile link. Deleted authors never
     * have profile links.
     *
     * Business Rules:
     * - If author == null → false (deleted user, no profile link)
     * - If author != null AND author.hasProfileLink() → true (active user with
     *                                                       profile visibility != HIDDEN)
     * - If author != null AND !author.hasProfileLink() → false (active user with
     *                                                           HIDDEN visibility)
     *
     * UI Usage: Use this to determine whether to render the author name as a clickable link
     * or plain text in thread lists and detail pages.
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
     * otherwise returns null. Use this method in conjunction with hasAuthorProfileLink()
     * to safely generate profile URLs.
     *
     * Usage Pattern:
     * if (thread.hasAuthorProfileLink()) {
     *     Long authorId = thread.getAuthorIdForLink();
     *     String profileUrl = "/users/" + authorId;
     *     // Render clickable link
     * } else {
     *     String authorName = thread.getAuthorDisplayName();
     *     // Render plain text
     * }
     *
     * Safety: This method guarantees null safety. It returns null for deleted authors
     * or users with HIDDEN visibility, preventing broken links.
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
