package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumVoteRepository extends JpaRepository<ForumVote, Integer> {

    List<ForumVote> findAll();

    ForumVote findById(int id);

    List<ForumVote> findByThread(int threadId);
}
