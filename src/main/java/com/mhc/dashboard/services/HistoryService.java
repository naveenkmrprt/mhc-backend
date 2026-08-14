package com.mhc.dashboard.services;

import com.mhc.dashboard.models.DailyLog;
import com.mhc.dashboard.models.QuizSession;
import com.mhc.dashboard.repositories.DailyLogRepository;
import com.mhc.dashboard.repositories.QuizSessionRepository;
import com.mhc.dashboard.repositories.SubTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final QuizSessionRepository quizSessionRepository;
    private final DailyLogRepository dailyLogRepository;
    private final SubTopicRepository subTopicRepository;

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        long totalSessions = quizSessionRepository.count();
        Double avgScore = quizSessionRepository.findAverageScore();
        Double avgAccuracy = quizSessionRepository.findAverageAccuracy();

        summary.put("totalSessions", totalSessions);
        summary.put("averageScore", avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0.0);
        summary.put("averageAccuracy", avgAccuracy != null ? Math.round(avgAccuracy * 100.0) / 100.0 : 0.0);

        long totalTopics = subTopicRepository.count();
        long completedTopics = subTopicRepository.findByIsCompleted(true).size();
        summary.put("syllabusCompletionPct",
                totalTopics > 0 ? Math.round((double) completedTopics / totalTopics * 100) : 0);

        List<QuizSession> recent = quizSessionRepository.findRecentSessions(PageRequest.of(0, 5));
        summary.put("recentSessions", recent.stream().map(s -> Map.of(
                "id", s.getId(),
                "score", s.getRawScore(),
                "accuracy", s.getAccuracyPct(),
                "date", s.getStartedAt() != null ? s.getStartedAt().toLocalDate().toString() : ""
        )).toList());

        return summary;
    }

    public List<DailyLog> getLast30DaysHistory() {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        return dailyLogRepository.findByLogDateBetweenOrderByLogDate(from, to);
    }

    public List<QuizSession> getAllSessions() {
        return quizSessionRepository.findAllByOrderByStartedAtDesc();
    }

    public Map<String, Object> getPartProgress() {
        Map<String, Object> progress = new LinkedHashMap<>();
        for (String part : new String[]{"A", "B", "C", "D"}) {
            long total = subTopicRepository.countByCategoryPart(part);
            long done = subTopicRepository.countCompletedByCategoryPart(part);
            progress.put("part" + part, Map.of(
                    "total", total,
                    "completed", done,
                    "pct", total > 0 ? Math.round((double) done / total * 100) : 0
            ));
        }
        return progress;
    }
}
