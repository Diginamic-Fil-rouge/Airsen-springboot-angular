package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumMessage;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumMessageRepository extends JpaRepository<ForumMessage, Integer> {

    List<ForumMessage> findAll();

    ForumMessage findById(int id);

    List<ForumMessage> findByThread(ForumThread thread);

    List<ForumMessage> findByAuthor(User author);
}
