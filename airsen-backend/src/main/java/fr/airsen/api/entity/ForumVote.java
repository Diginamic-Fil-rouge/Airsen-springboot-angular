package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.VoteType;
import jakarta.persistence.*;

import java.util.Objects;

/**
 * This entity represents a vote in the forum. It is linked to a {@link User } entity and a {@link ForumThread } entity.
 */
@Entity
@Table(name = "forum_votes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "thread_id"})
    },
    indexes = {
        @Index(name = "idx_vote_user_deleted", columnList = "user_deleted")
    }
)
public class ForumVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * The user who cast this vote.
     *
     * GDPR Compliance:vThis field is nullable to support voter anonymization
     * after user deletion. When a user is deleted (GDPR right to erasure), this field is set to
     * null to remove voter identity while preserving the vote count.
     *
     * Business Rules:
     *   If user != null → active voter, vote counts toward thread score
     *   If user == null AND userDeleted == true → deleted voter, vote count preserved
     *       but voter identity anonymized
     *
     *  Unlike ForumThread/ForumMessage, votes do NOT preserve user
     * names. Voter anonymization is complete - only the vote type (UPVOTE/DOWNVOTE) is kept.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * Flag indicating whether the voter has been deleted.
     */
    @Column(name = "user_deleted", nullable = false)
    private Boolean userDeleted = false;

    @ManyToOne
    @JoinColumn(name = "thread_id", nullable = false)
    private ForumThread thread;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    private VoteType voteType;

    public ForumVote() {
    }

    public ForumVote(User user, ForumThread thread, VoteType voteType) {
        this.user = user;
        this.userDeleted = false;  // New votes always have active users
        this.thread = thread;
        this.voteType = voteType;
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

    public ForumThread getThread() {
        return thread;
    }

    public void setThread(ForumThread thread) {
        this.thread = thread;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }

    public Boolean getUserDeleted() {
        return userDeleted;
    }

    public void setUserDeleted(Boolean userDeleted) {
        this.userDeleted = userDeleted;
    }

    @Override
    public String toString() {
        return "ForumVote{" +
                "id=" + id +
                ", user=" + user +
                ", thread=" + thread +
                ", voteType=" + voteType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ForumVote forumVote)) return false;
        return Objects.equals(id, forumVote.id) && Objects.equals(user, forumVote.user) && Objects.equals(thread, forumVote.thread);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, thread);
    }
}
