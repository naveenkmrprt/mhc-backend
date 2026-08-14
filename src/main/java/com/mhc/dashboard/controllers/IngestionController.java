package com.mhc.dashboard.controllers;

import com.mhc.dashboard.models.Question;
import com.mhc.dashboard.services.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Bulk Question Ingestion API.
 *
 * Endpoints:
 *   POST /api/v1/ingest/text      — Raw text block (Q:, A:, B:, C:, D:, ANS:, TOPIC: format)
 *   POST /api/v1/ingest/file      — Upload a .txt file with same format
 *   POST /api/v1/ingest/json      — JSON array of question objects
 */
@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    /**
     * Ingest from a raw text body.
     * Body: { "text": "Q: ...\nA: ...\nANS: A\n---\nQ: ...", "source": "MANUAL" }
     */
    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> ingestFromText(@RequestBody Map<String, String> body) {
        String rawText = body.getOrDefault("text", "");
        String source  = body.getOrDefault("source", "MANUAL");

        if (rawText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text body is empty."));
        }

        List<Question> saved = ingestionService.ingestFromText(rawText, source);
        return ResponseEntity.ok(Map.of(
                "message", "Ingestion successful.",
                "count", saved.size(),
                "savedIds", saved.stream().map(Question::getId).toList()
        ));
    }

    /**
     * Ingest from a plain text file upload.
     */
    @PostMapping("/file")
    public ResponseEntity<Map<String, Object>> ingestFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", defaultValue = "FILE_UPLOAD") String source) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty."));
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        List<Question> saved = ingestionService.ingestFromText(content, source);
        return ResponseEntity.ok(Map.of(
                "message", "File ingested successfully.",
                "filename", file.getOriginalFilename(),
                "count", saved.size()
        ));
    }

    /**
     * Ingest from a JSON array.
     * Body: [ { "question": "...", "A": "...", "B": "...", "C": "...", "D": "...", "answer": "A", "topic": "OSI Model", "source": "NIC_2021" } ]
     */
    @PostMapping("/json")
    public ResponseEntity<Map<String, Object>> ingestFromJson(
            @RequestBody List<Map<String, String>> questions) {

        if (questions == null || questions.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "JSON question list is empty."));
        }

        List<Question> saved = ingestionService.ingestFromJsonList(questions);
        return ResponseEntity.ok(Map.of(
                "message", "JSON ingestion successful.",
                "count", saved.size()
        ));
    }
}
