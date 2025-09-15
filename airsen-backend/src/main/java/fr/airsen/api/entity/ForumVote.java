package fr.airsen.api.entity;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * This entity represents a vote in the forum. It is linked to a {@link User } entity and a {@link ForumThread } entity.
 */
@Entity
public class ForumVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private User user;

    @ManyToOne
    private ForumThread thread;

    private boolean like;

    public ForumVote() {
    }

    public ForumVote(User user, ForumThread thread, boolean like) {
        this.user = user;
        this.thread = thread;
        this.like = like;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public boolean isLike() {
        return like;
    }

    public void setLike(boolean like) {
        this.like = like;
    }

    @Override
    public String toString() {
        return "ForumVote{" +
                "id=" + id +
                ", user=" + user +
                ", thread=" + thread +
                ", like=" + like +
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
