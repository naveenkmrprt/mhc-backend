package com.mhc.dashboard.services;

import com.mhc.dashboard.models.Question;
import com.mhc.dashboard.models.SyllabusCategory;
import com.mhc.dashboard.repositories.QuestionRepository;
import com.mhc.dashboard.repositories.SyllabusCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw text blocks into Question entities using regex.
 *
 * Supported format:
 *   Q: What is the OSI model?
 *   A: Open Systems Interconnection
 *   B: Open Source Interface
 *   C: Operating System Interface
 *   D: None of the above
 *   ANS: A
 *   TOPIC: OSI Model & TCP/IP Stack
 *   DIFFICULTY: MEDIUM
 *   SOURCE: TNPSC_SE_2022
 *
 * Fields TOPIC, DIFFICULTY, SOURCE are optional (defaults applied).
 * Multiple questions can be separated by blank lines or "---".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final QuestionRepository questionRepository;
    private final SyllabusCategoryRepository syllabusCategoryRepository;

    // Core parsing patterns
    private static final Pattern Q_PATTERN      = Pattern.compile("(?i)^Q:\\s*(.+)$");
    private static final Pattern A_PATTERN      = Pattern.compile("(?i)^A:\\s*(.+)$");
    private static final Pattern B_PATTERN      = Pattern.compile("(?i)^B:\\s*(.+)$");
    private static final Pattern C_PATTERN      = Pattern.compile("(?i)^C:\\s*(.+)$");
    private static final Pattern D_PATTERN      = Pattern.compile("(?i)^D:\\s*(.+)$");
    private static final Pattern ANS_PATTERN    = Pattern.compile("(?i)^ANS:\\s*([ABCD])$");
    private static final Pattern TOPIC_PATTERN  = Pattern.compile("(?i)^TOPIC:\\s*(.+)$");
    private static final Pattern DIFF_PATTERN   = Pattern.compile("(?i)^DIFFICULTY:\\s*(EASY|MEDIUM|HARD)$");
    private static final Pattern SRC_PATTERN    = Pattern.compile("(?i)^SOURCE:\\s*(.+)$");

    /**
     * Parse a raw text block and save all valid questions to the database.
     * Returns the list of saved Question objects.
     */
    @Transactional
    public List<Question> ingestFromText(String rawText, String defaultSource) {
        List<Question> saved = new ArrayList<>();

        // Split into individual question blocks (by blank line or ---)
        String[] blocks = rawText.split("(?m)^\\s*---+\\s*$|(?:\\r?\\n){2,}");

        for (String block : blocks) {
            if (block.isBlank()) continue;
            try {
                Question q = parseBlock(block.trim(), defaultSource);
                if (q != null) {
                    saved.add(questionRepository.save(q));
                }
            } catch (Exception e) {
                log.warn("Skipping block due to parse error: {}", e.getMessage());
            }
        }

        log.info("Ingestion complete: {} questions saved from text block.", saved.size());
        return saved;
    }

    /**
     * Ingest a list of pre-structured JSON-style maps (from JSON upload).
     */
    @Transactional
    public List<Question> ingestFromJsonList(List<Map<String, String>> questionMaps) {
        List<Question> saved = new ArrayList<>();

        for (Map<String, String> map : questionMaps) {
            try {
                Question q = new Question();
                q.setQuestionText(map.getOrDefault("question", "").trim());
                q.setOptionA(map.getOrDefault("optionA", map.getOrDefault("A", "")));
                q.setOptionB(map.getOrDefault("optionB", map.getOrDefault("B", "")));
                q.setOptionC(map.getOrDefault("optionC", map.getOrDefault("C", "")));
                q.setOptionD(map.getOrDefault("optionD", map.getOrDefault("D", "")));
                q.setCorrectOption(map.getOrDefault("answer", map.getOrDefault("ANS", "A")).toUpperCase().trim());
                q.setDifficultyEstimate(map.getOrDefault("difficulty", "MEDIUM").toUpperCase());
                q.setSourceDocument(map.getOrDefault("source", "JSON_IMPORT"));

                String topicName = map.getOrDefault("topic", "");
                if (!topicName.isBlank()) {
                    q.setSyllabusCategory(findOrCreateTopic(topicName));
                }

                if (isValidQuestion(q)) {
                    saved.add(questionRepository.save(q));
                }
            } catch (Exception e) {
                log.warn("Skipping JSON question due to error: {}", e.getMessage());
            }
        }

        log.info("JSON Ingestion complete: {} questions saved.", saved.size());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Question parseBlock(String block, String defaultSource) {
        Question q = new Question();
        String topicName = null;

        for (String line : block.lines().toList()) {
            line = line.trim();
            Matcher m;

            if ((m = Q_PATTERN.matcher(line)).matches())     q.setQuestionText(m.group(1));
            else if ((m = A_PATTERN.matcher(line)).matches()) q.setOptionA(m.group(1));
            else if ((m = B_PATTERN.matcher(line)).matches()) q.setOptionB(m.group(1));
            else if ((m = C_PATTERN.matcher(line)).matches()) q.setOptionC(m.group(1));
            else if ((m = D_PATTERN.matcher(line)).matches()) q.setOptionD(m.group(1));
            else if ((m = ANS_PATTERN.matcher(line)).matches()) q.setCorrectOption(m.group(1).toUpperCase());
            else if ((m = TOPIC_PATTERN.matcher(line)).matches()) topicName = m.group(1).trim();
            else if ((m = DIFF_PATTERN.matcher(line)).matches()) q.setDifficultyEstimate(m.group(1).toUpperCase());
            else if ((m = SRC_PATTERN.matcher(line)).matches()) q.setSourceDocument(m.group(1).trim());
        }

        if (!isValidQuestion(q)) return null;

        q.setSourceDocument(q.getSourceDocument() != null ? q.getSourceDocument() : defaultSource);
        q.setDifficultyEstimate(q.getDifficultyEstimate() != null ? q.getDifficultyEstimate() : "MEDIUM");

        if (topicName != null) {
            q.setSyllabusCategory(findOrCreateTopic(topicName));
        }

        return q;
    }

    @Transactional
    public List<Question> parseAndSaveOfficial(String rawText, Long topicId) {
        List<Question> parsed = new ArrayList<>();
        String[] blocks = rawText.split("(?m)^\\s*---+\\s*$|(?:\\r?\\n){2,}");
        for (String block : blocks) {
            if (block.isBlank()) continue;
            Question q = parseBlock(block.trim(), "MHC_2021");
            if (q != null) {
                q.setDifficultyEstimate("MEDIUM");
                q.setSourceDocument("MANUAL");
                if (topicId != null) {
                    SyllabusCategory cat = new SyllabusCategory();
                    cat.setId(topicId);
                    q.setSyllabusCategory(cat);
                }
                parsed.add(q);
            }
        }
        return questionRepository.saveAll(parsed);
    }

    private SyllabusCategory findOrCreateTopic(String topicName) {
        return syllabusCategoryRepository.findAll()
                .stream().filter(c -> c.getName().equalsIgnoreCase(topicName)).findFirst()
                .orElseGet(() -> {
                    SyllabusCategory t = new SyllabusCategory();
                    t.setName(topicName);
                    t.setPart("GEN"); // Fallback part
                    t.setTotalMarks(0);
                    log.info("Creating new topic on-the-fly: {}", topicName);
                    return syllabusCategoryRepository.save(t);
                });
    }

    private boolean isValidQuestion(Question q) {
        return q.getQuestionText() != null && !q.getQuestionText().isBlank()
            && q.getOptionA() != null && !q.getOptionA().isBlank()
            && q.getOptionB() != null && !q.getOptionB().isBlank()
            && q.getCorrectOption() != null
            && List.of("A", "B", "C", "D").contains(q.getCorrectOption().toUpperCase());
    }
}
