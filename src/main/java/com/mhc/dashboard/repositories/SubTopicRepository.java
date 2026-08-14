package com.mhc.dashboard.repositories;

import com.mhc.dashboard.models.SubTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface SubTopicRepository extends JpaRepository<SubTopic, Long> {

    List<SubTopic> findByIsCompleted(Boolean isCompleted);

    @Query("SELECT COUNT(s) FROM SubTopic s WHERE s.category.part = :part")
    long countByCategoryPart(@Param("part") String part);

    @Query("SELECT COUNT(s) FROM SubTopic s WHERE s.category.part = :part AND s.isCompleted = true")
    long countCompletedByCategoryPart(@Param("part") String part);
}
