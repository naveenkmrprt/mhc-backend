package com.mhc.dashboard.controllers;

import com.mhc.dashboard.models.SyllabusCategory;
import com.mhc.dashboard.models.SubTopic;
import com.mhc.dashboard.repositories.SyllabusCategoryRepository;
import com.mhc.dashboard.repositories.SubTopicRepository;
import com.mhc.dashboard.services.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final SyllabusCategoryRepository categoryRepository;
    private final SubTopicRepository subTopicRepository;
    private final HistoryService historyService;

    @GetMapping("/syllabus")
    public ResponseEntity<List<SyllabusCategory>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping("/topic/{id}/toggle")
    public ResponseEntity<SubTopic> toggleTopic(@PathVariable Long id) {
        SubTopic topic = subTopicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubTopic not found: " + id));
        topic.setIsCompleted(!topic.getIsCompleted());
        return ResponseEntity.ok(subTopicRepository.save(topic));
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(historyService.getPartProgress());
    }
}
