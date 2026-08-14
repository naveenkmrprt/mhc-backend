package com.mhc.dashboard.repositories;

import com.mhc.dashboard.models.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    List<QuizSession> findAllByOrderByStartedAtDesc();

    List<QuizSession> findBySessionType(String sessionType);

    @Query("SELECT AVG(q.rawScore) FROM QuizSession q")
    Double findAverageScore();

    @Query("SELECT AVG(q.accuracyPct) FROM QuizSession q")
    Double findAverageAccuracy();

    @Query("SELECT q FROM QuizSession q ORDER BY q.startedAt DESC")
    List<QuizSession> findRecentSessions(org.springframework.data.domain.Pageable pageable);
}
