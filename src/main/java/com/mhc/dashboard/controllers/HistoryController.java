package com.mhc.dashboard.controllers;

import com.mhc.dashboard.models.DailyLog;
import com.mhc.dashboard.models.QuizSession;
import com.mhc.dashboard.services.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Performance History & Analytics API.
 *
 * GET /api/v1/history/summary    — Dashboard overview stats
 * GET /api/v1/history/logs       — Last 30 days daily logs (for chart)
 * GET /api/v1/history/sessions   — All quiz sessions in reverse chron order
 * GET /api/v1/history/progress   — Part-by-part syllabus completion
 */
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        return ResponseEntity.ok(historyService.getDashboardSummary());
    }

    @GetMapping("/logs")
    public ResponseEntity<List<DailyLog>> getLast30Days() {
        return ResponseEntity.ok(historyService.getLast30DaysHistory());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<QuizSession>> getAllSessions() {
        return ResponseEntity.ok(historyService.getAllSessions());
    }

    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getPartProgress() {
        return ResponseEntity.ok(historyService.getPartProgress());
    }
}
