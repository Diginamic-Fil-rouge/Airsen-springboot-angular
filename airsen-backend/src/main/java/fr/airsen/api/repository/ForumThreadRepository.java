package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import fr.airsen.api.entity.ForumThread;
import fr.airsen.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Integer> {

    List<ForumThread> findAll();

    ForumThread findById(int id);

    List<ForumThread> findByCategory(ForumCategory category);

    List<ForumThread> findByAuthor(User author);
}
