package com.mhc.dashboard.repositories;

import com.mhc.dashboard.models.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findBySessionId(Long sessionId);
    java.util.Optional<QuizAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
}
