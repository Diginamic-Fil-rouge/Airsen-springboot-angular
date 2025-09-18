package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.VoteType;
import jakarta.persistence.*;

import java.util.Objects;

/**
 * This entity represents a vote in the forum. It is linked to a {@link User } entity and a {@link ForumThread } entity.
 */
@Entity
public class ForumVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
