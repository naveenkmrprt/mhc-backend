package com.mhc.dashboard.tools;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.*;

/**
 * One-shot PDF syllabus extractor.
 * Uses two independent libraries:
 *   1. Apache PDFBox 3.x  → full raw text extraction (primary)
 *   2. Tabula-java 1.0.5  → table-aware extraction (cross-verify)
 *
 * Run with:  mvn compile exec:java
 * Output written to: target/extracted-syllabus.txt  +  target/data-extracted.sql
 */
public class PdfSyllabusExtractor {

    private static final String PDF_PATH =
        "src/main/java/com/mhc/dashboard/syllabus-aka-notification/" +
        "Madras-High-Court-Assistant-Programmer-Recruitment-2025.pdf";

    public static void main(String[] args) throws Exception {
        File pdfFile = new File(PDF_PATH);
        if (!pdfFile.exists()) {
            System.err.println("ERROR: PDF not found at " + pdfFile.getAbsolutePath());
            System.exit(1);
        }

        System.out.println("=".repeat(70));
        System.out.println("MHC AP SYLLABUS EXTRACTOR");
        System.out.println("PDF: " + pdfFile.getAbsolutePath());
        System.out.println("Size: " + pdfFile.length() / 1024 + " KB");
        System.out.println("=".repeat(70));

        // ── 1. PDFBOX — full text dump ────────────────────────────────────────
        String pdfBoxText = extractWithPdfBox(pdfFile);
        System.out.println("\n[PDFBOX] Extracted " + pdfBoxText.lines().count() + " lines of text");

        // ── 2. TABULA — table extraction ──────────────────────────────────────
        String tabulaText = extractWithTabula(pdfFile);
        System.out.println("[TABULA] Extracted " + tabulaText.lines().count() + " lines from tables");

        // ── 3. Write raw dumps for inspection ─────────────────────────────────
        new File("target").mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter("target/pdfbox-raw.txt"))) {
            pw.println("=".repeat(70));
            pw.println("PDFBOX RAW EXTRACTION");
            pw.println("=".repeat(70));
            pw.println(pdfBoxText);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter("target/tabula-raw.txt"))) {
            pw.println("=".repeat(70));
            pw.println("TABULA TABLE EXTRACTION");
            pw.println("=".repeat(70));
            pw.println(tabulaText);
        }

        // ── 4. Parse topics from both extractions ─────────────────────────────
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PARSING SYLLABUS TOPICS...");
        System.out.println("=".repeat(70));

        List<SyllabusTopic> pdfBoxTopics  = parseTopics(pdfBoxText,  "PDFBOX");
        List<SyllabusTopic> tabulaTopics  = parseTopics(tabulaText,  "TABULA");

        // Cross-verify and merge
        List<SyllabusTopic> mergedTopics  = crossVerifyAndMerge(pdfBoxTopics, tabulaTopics);

        // ── 5. Generate SQL ────────────────────────────────────────────────────
        String sql = generateSql(mergedTopics);

        try (PrintWriter pw = new PrintWriter(new FileWriter("target/data-extracted.sql"))) {
            pw.println(sql);
        }

        System.out.println("\n" + "=".repeat(70));
        System.out.println("DONE. Output files:");
        System.out.println("  target/pdfbox-raw.txt     ← full PDFBox dump");
        System.out.println("  target/tabula-raw.txt     ← Tabula table dump");
        System.out.println("  target/data-extracted.sql ← generated seed SQL");
        System.out.println("=".repeat(70));

        // Print the SQL to stdout as well so we can copy it
        System.out.println("\n--- GENERATED SQL (also in target/data-extracted.sql) ---\n");
        System.out.println(sql);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDFBox Extraction
    // ─────────────────────────────────────────────────────────────────────────
    private static String extractWithPdfBox(File pdfFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tabula Extraction
    // ─────────────────────────────────────────────────────────────────────────
    private static String extractWithTabula(File pdfFile) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            ObjectExtractor extractor = new ObjectExtractor(doc);
            PageIterator pageIt = extractor.extract();

            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
            BasicExtractionAlgorithm    bea = new BasicExtractionAlgorithm();

            int pageNum = 1;
            while (pageIt.hasNext()) {
                Page page = pageIt.next();

                // Try spreadsheet algorithm first (best for ruled tables)
                List<Table> tables = sea.extract(page);

                // If no structured tables found, fall back to basic
                if (tables.isEmpty()) {
                    tables = bea.extract(page);
                }

                if (!tables.isEmpty()) {
                    sb.append("\n--- PAGE ").append(pageNum).append(" ---\n");
                    for (Table table : tables) {
                        for (List<RectangularTextContainer> row : table.getRows()) {
                            StringBuilder rowSb = new StringBuilder();
                            for (RectangularTextContainer cell : row) {
                                String text = cell.getText().trim().replaceAll("\\s+", " ");
                                rowSb.append(text).append(" | ");
                            }
                            String rowStr = rowSb.toString().trim();
                            if (!rowStr.isEmpty() && !rowStr.equals("|")) {
                                sb.append(rowStr).append("\n");
                            }
                        }
                    }
                }
                pageNum++;
            }
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Topic Parser — applies heuristics to identify Parts and topics
    // ─────────────────────────────────────────────────────────────────────────
    private static final Pattern PART_A = Pattern.compile(
        "(?i)(part[\\s-]*a|general\\s+english|paper[\\s-]*i)", Pattern.MULTILINE);
    private static final Pattern PART_B = Pattern.compile(
        "(?i)(part[\\s-]*b|general\\s+tamil|tamil\\s+language)", Pattern.MULTILINE);
    private static final Pattern PART_C = Pattern.compile(
        "(?i)(part[\\s-]*c|general\\s+knowledge|general\\s+awareness|aptitude|reasoning)", Pattern.MULTILINE);
    private static final Pattern PART_D = Pattern.compile(
        "(?i)(part[\\s-]*d|computer\\s+science|technical|programming|software)", Pattern.MULTILINE);

    // Lines that look like numbered topic entries
    private static final Pattern TOPIC_LINE = Pattern.compile(
        "^\\s*(?:\\d+[.)\\s]+|[-*•●]\\s+|[ivxIVX]+[.)\\s]+)?(.{10,120})\\s*$", Pattern.MULTILINE);

    // Lines that are clearly headings/page markers/noise
    private static final Pattern NOISE = Pattern.compile(
        "(?i)(page\\s+\\d|www\\.|notification|recruitment|madras\\s+high\\s+court\\s+assistant|" +
        "signature|category|post\\s+code|total\\s+vacancies|application\\s+fee|" +
        "^\\s*\\d+\\s*$|^\\s*[.,-]+\\s*$|salary|pay\\s+scale|age\\s+limit|" +
        "how\\s+to\\s+apply|important\\s+date|selection\\s+process)");

    private static List<SyllabusTopic> parseTopics(String text, String source) {
        List<SyllabusTopic> topics = new ArrayList<>();
        String currentPart = "D"; // default to Part D (Computer Knowledge = most topics)
        int partDMarks = 100;

        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Detect part transitions
            if (PART_A.matcher(line).find()) { currentPart = "A"; continue; }
            if (PART_B.matcher(line).find()) { currentPart = "B"; continue; }
            if (PART_C.matcher(line).find()) { currentPart = "C"; continue; }
            if (PART_D.matcher(line).find()) { currentPart = "D"; continue; }

            // Skip noise
            if (NOISE.matcher(line).find()) continue;

            // Skip very short lines
            if (line.length() < 8) continue;

            // Skip lines that are all caps and look like headers (> 3 words all caps)
            String[] words = line.split("\\s+");
            boolean allCaps = words.length > 3 &&
                Arrays.stream(words).allMatch(w -> w.equals(w.toUpperCase()));
            if (allCaps && currentPart.equals("D")) continue; // skip section headers in Part D

            // Extract marks if mentioned inline
            int marks = switch (currentPart) {
                case "A" -> 15;
                case "B" -> 15;
                case "C" -> 20;
                default  -> 100;
            };
            boolean negativeMarking = currentPart.equals("D") || currentPart.equals("C");

            // Clean the topic name
            String topicName = line
                .replaceAll("^\\d+[.)\\s]+", "")       // remove leading numbers
                .replaceAll("^[-*•●]\\s*", "")          // remove bullets
                .replaceAll("[\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

            if (topicName.length() >= 8 && topicName.length() <= 120) {
                topics.add(new SyllabusTopic(currentPart, topicName, marks, negativeMarking));
                System.out.printf("[%s][Part %s] %s%n", source, currentPart, topicName);
            }
        }
        return topics;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cross-verify: merge PDFBox + Tabula, deduplicate
    // ─────────────────────────────────────────────────────────────────────────
    private static List<SyllabusTopic> crossVerifyAndMerge(
            List<SyllabusTopic> primary, List<SyllabusTopic> secondary) {

        System.out.println("\n=== CROSS-VERIFICATION ===");
        System.out.println("PDFBox topics: " + primary.size());
        System.out.println("Tabula topics: " + secondary.size());

        Map<String, SyllabusTopic> merged = new LinkedHashMap<>();

        // Add all primary first
        for (SyllabusTopic t : primary) {
            String key = normalizeKey(t.name);
            merged.put(key, t);
        }

        // Add secondary only if not already present (cross-verify additions)
        int newFromTabula = 0;
        for (SyllabusTopic t : secondary) {
            String key = normalizeKey(t.name);
            if (!merged.containsKey(key)) {
                merged.put(key, t);
                System.out.println("[TABULA-ONLY] " + t.part + " | " + t.name);
                newFromTabula++;
            }
        }

        System.out.println("New topics found only by Tabula: " + newFromTabula);
        System.out.println("Total merged topics: " + merged.size());

        return new ArrayList<>(merged.values());
    }

    private static String normalizeKey(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9]", "")
            .trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SQL Generator
    // ─────────────────────────────────────────────────────────────────────────
    private static String generateSql(List<SyllabusTopic> topics) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- ================================================================\n");
        sb.append("-- AUTO-GENERATED from official MHC AP Notification 2025 PDF\n");
        sb.append("-- Extracted using: Apache PDFBox 3.0.2 + Tabula-java 1.0.5\n");
        sb.append("-- Topics extracted: ").append(topics.size()).append("\n");
        sb.append("-- ================================================================\n\n");

        sb.append("INSERT INTO syllabus_topics\n");
        sb.append("    (part, name, total_marks, negative_marking, is_completed, created_at)\n");
        sb.append("SELECT part, name, total_marks, negative_marking, is_completed, created_at\n");
        sb.append("FROM (VALUES\n");

        Map<String, List<SyllabusTopic>> byPart = new LinkedHashMap<>();
        byPart.put("A", new ArrayList<>());
        byPart.put("B", new ArrayList<>());
        byPart.put("C", new ArrayList<>());
        byPart.put("D", new ArrayList<>());
        for (SyllabusTopic t : topics) {
            byPart.getOrDefault(t.part, byPart.get("D")).add(t);
        }

        boolean first = true;
        for (Map.Entry<String, List<SyllabusTopic>> entry : byPart.entrySet()) {
            String part = entry.getKey();
            List<SyllabusTopic> partTopics = entry.getValue();
            if (partTopics.isEmpty()) continue;

            sb.append("\n    -- Part ").append(part).append("\n");
            for (SyllabusTopic t : partTopics) {
                if (!first) sb.append(",\n");
                String escaped = t.name.replace("'", "''");
                sb.append(String.format("    ('%s', '%s', %d, %s, FALSE, CURRENT_TIMESTAMP)",
                    part, escaped, t.marks, t.negativeMarking));
                first = false;
            }
        }

        sb.append("\n) AS t(part, name, total_marks, negative_marking, is_completed, created_at)\n");
        sb.append("WHERE NOT EXISTS (SELECT 1 FROM syllabus_topics LIMIT 1);\n");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data class
    // ─────────────────────────────────────────────────────────────────────────
    record SyllabusTopic(String part, String name, int marks, boolean negativeMarking) {}
}
