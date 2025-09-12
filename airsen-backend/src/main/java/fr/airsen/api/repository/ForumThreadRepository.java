package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Integer> {

    List<ForumThread> findAll();

    ForumThread findById(int id);

    List<ForumThread> findByCategory(int categoryId);

    List<ForumThread> findByAuthor(int authorId);
}
