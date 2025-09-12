package fr.airsen.api.repository;

import fr.airsen.api.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Integer> {

    List<ForumCategory> findAll();

    ForumCategory findById(int id);
}
