package com.mhc.dashboard.repositories;

import com.mhc.dashboard.models.Question;
import com.mhc.dashboard.models.SyllabusCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySyllabusCategory(SyllabusCategory syllabusCategory);

    List<Question> findBySyllabusCategoryId(Long categoryId);

    List<Question> findBySourceDocument(String sourceDocument);

    @Query(value = "SELECT * FROM questions WHERE verification_status = :status ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Question> findRandomByStatus(String status, int count);

    @Query(value = "SELECT * FROM questions WHERE category_id = :categoryId AND verification_status IN (:statuses) ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Question> findRandomByCategoryAndStatusIn(Long categoryId, List<String> statuses, int count);

    @Query(value = "SELECT * FROM questions WHERE category_id IN " +
           "(SELECT id FROM syllabus_categories WHERE part = :part) AND verification_status IN (:statuses) ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Question> findRandomByPartAndStatusIn(String part, List<String> statuses, int count);

    long countBySyllabusCategory(SyllabusCategory syllabusCategory);
}
