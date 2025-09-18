package fr.airsen.api.dto;

import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.User;


import java.time.LocalDateTime;

public class ForumMessageDTO {

    private long id;

    private User author;

    private ForumThreadDTO thread;

    private String content;

    private LocalDateTime createdDate;

    public ForumMessageDTO() {
    }

    public ForumMessageDTO(ForumMessage forumMessage, boolean withThread){
        this.id = forumMessage.getId();
        this.author = forumMessage.getAuthor();
        // TODO after UserDTO implementation
//        this.author = new UserDTO(forumMessage.getAuthor());
        this.content = forumMessage.getContent();
        this.createdDate = forumMessage.getCreatedDate();
        if(withThread){
            this.thread = new ForumThreadDTO(forumMessage.getThread(), false);
        }
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

    public ForumThreadDTO getThread() {
        return thread;
    }

    public void setThread(ForumThreadDTO thread) {
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
}
