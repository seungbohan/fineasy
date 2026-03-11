package com.fineasy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfTermExtractor {

    private static final String OUTPUT_PATH = "src/main/resources/data/bok-financial-terms.json";

    private static final Pattern TERM_WITH_ENGLISH = Pattern.compile(
            "^([가-힣][가-힣A-Za-z0-9+·\\-/\\s()]+?)\\s*[\\(（]([A-Za-z][A-Za-z0-9\\s\\-·,/&'.;:()]+)[\\)）]\\s*$"
    );

    private static final Pattern KOREAN_TERM_ONLY = Pattern.compile(
            "^([가-힣][가-힣A-Za-z0-9+·\\-/\\s]{1,25})$"
    );

    private static final Set<String> SKIP_EXACT = Set.of(
            "경제금융용어 700선", "찾아보기"
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== BOK Financial Terms PDF Extractor ===");

        File pdfFile = resolvePdfFile();
        System.out.println("Input PDF: " + pdfFile.getAbsolutePath());
        System.out.printf("PDF file size: %.1f MB%n", pdfFile.length() / 1_000_000.0);

        System.out.println("\nStep 1: Extracting text from PDF (pages 19-375)...");
        String fullText = extractText(pdfFile, 19, 375);
        System.out.printf("Extracted %,d characters of text.%n", fullText.length());

        System.out.println("\nStep 2: Parsing terms...");
        List<Map<String, String>> terms = parseTerms(fullText);
        System.out.printf("Parsed %d terms.%n", terms.size());

        System.out.println("\nStep 3: Writing JSON output...");
        writeJson(terms);

        System.out.println("\nDone! Extracted " + terms.size() + " terms.");

        System.out.println("\n=== First 10 terms ===");
        for (int i = 0; i < Math.min(10, terms.size()); i++) {
            Map<String, String> t = terms.get(i);
            String def = t.get("definition");
            if (def.length() > 80) def = def.substring(0, 80) + "...";
            System.out.printf("[%3d] %s (%s)%n      %s%n",
                    i + 1, t.get("term"), t.get("englishTerm"), def);
        }

        System.out.println("\n=== Last 5 terms ===");
        for (int i = Math.max(0, terms.size() - 5); i < terms.size(); i++) {
            Map<String, String> t = terms.get(i);
            String def = t.get("definition");
            if (def.length() > 80) def = def.substring(0, 80) + "...";
            System.out.printf("[%3d] %s (%s)%n      %s%n",
                    i + 1, t.get("term"), t.get("englishTerm"), def);
        }
    }

    private static String extractText(File pdfFile, int startPage, int endPage) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(startPage);
            stripper.setEndPage(Math.min(endPage, document.getNumberOfPages()));
            return stripper.getText(document);
        }
    }

    private static List<Map<String, String>> parseTerms(String fullText) {
        List<Map<String, String>> terms = new ArrayList<>();
        String[] lines = fullText.split("\\r?\\n");

        String currentTerm = null;
        String currentEnglish = null;
        StringBuilder currentDef = new StringBuilder();

        String currentSection = "ㄱ";

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty()) continue;

            if (line.matches("^\\d{1,4}$")) continue;

            if (SKIP_EXACT.contains(line)) continue;

            if (line.matches("^[ㄱ-ㅎ]$")) {
                currentSection = line;
                continue;
            }

            if (line.startsWith("연관검색어") || line.startsWith("연관 검색어")) {
                continue;
            }

            if (line.matches(".*[ㄱ-ㅎ]\\s*$") && line.length() < 30) {
                continue;
            }

            Matcher withEnglish = TERM_WITH_ENGLISH.matcher(line);
            if (withEnglish.matches()) {

                saveTerm(terms, currentTerm, currentEnglish,
                        currentDef.toString().trim(), currentSection);

                currentTerm = withEnglish.group(1).trim();
                currentEnglish = withEnglish.group(2).trim();
                currentDef = new StringBuilder();
                continue;
            }

            Matcher koreanOnly = KOREAN_TERM_ONLY.matcher(line);
            if (koreanOnly.matches() && isProbablyTermHeading(line, currentDef)) {

                saveTerm(terms, currentTerm, currentEnglish,
                        currentDef.toString().trim(), currentSection);

                currentTerm = koreanOnly.group(1).trim();
                currentEnglish = "";
                currentDef = new StringBuilder();
                continue;
            }

            if (currentTerm != null) {
                if (!currentDef.isEmpty()) {
                    currentDef.append(" ");
                }
                currentDef.append(line);
            }
        }

        saveTerm(terms, currentTerm, currentEnglish,
                currentDef.toString().trim(), currentSection);

        postProcess(terms);

        return terms;
    }

    private static boolean isProbablyTermHeading(String line, StringBuilder currentDef) {

        if (line.length() > 25) return false;

        if (line.endsWith("다.") || line.endsWith("다") || line.endsWith("있다.")
                || line.endsWith("한다.") || line.endsWith("된다.")) return false;

        String[] defPhrases = {"을 말한다", "를 말한다", "이란", "란 ", "을 의미",
                "를 의미", "에 대한", "으로서", "에 따라", "을 하는", "를 하는"};
        for (String p : defPhrases) {
            if (line.contains(p)) return false;
        }

        if (currentDef.length() > 50) return true;

        if (line.contains("/") || line.contains("·")) return true;

        return line.length() <= 15;
    }

    private static void saveTerm(List<Map<String, String>> terms,
                                  String term, String english,
                                  String definition, String section) {
        if (term == null || term.isEmpty()) return;
        if (definition == null || definition.length() < 30) return;

        definition = definition.replaceAll("\\s+", " ").trim();
        term = term.replaceAll("\\s+", " ").trim();

        definition = definition.replaceAll("\\s*\\d{1,3}\\s*$", "").trim();

        String category = sectionToCategory(section);

        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("term", term);
        entry.put("englishTerm", english != null ? english.trim() : "");
        entry.put("definition", definition);
        entry.put("category", category);

        terms.add(entry);
    }

    private static void postProcess(List<Map<String, String>> terms) {

        terms.removeIf(t -> t.get("definition").length() < 50);

        terms.removeIf(t -> t.get("term").length() > 25);

        terms.removeIf(t -> {
            String term = t.get("term");
            return term.startsWith("가 ") || term.startsWith("을 ") || term.startsWith("를 ")
                    || term.startsWith("이 ") || term.startsWith("의 ");
        });

        Set<String> seen = new LinkedHashSet<>();
        terms.removeIf(t -> !seen.add(t.get("term")));

        terms.sort(Comparator.comparing(t -> t.get("term")));
    }

    private static String sectionToCategory(String section) {
        return "경제금융일반";
    }

    private static void writeJson(List<Map<String, String>> terms) throws IOException {
        Path outputPath = Paths.get(OUTPUT_PATH);
        Files.createDirectories(outputPath.getParent());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outputPath.toFile(), terms);

        System.out.printf("JSON written to: %s (%,d bytes)%n",
                outputPath.toAbsolutePath(), Files.size(outputPath));
    }

    private static File resolvePdfFile() {
        String[] candidates = {
                System.getenv("TEMP") + File.separator + "bok_terms.pdf",
                System.getenv("TMP") + File.separator + "bok_terms.pdf",
                System.getProperty("java.io.tmpdir") + File.separator + "bok_terms.pdf",
                "C:\\Users\\computer\\AppData\\Local\\Temp\\bok_terms.pdf",
                "/tmp/bok_terms.pdf"
        };
        for (String c : candidates) {
            if (c != null) {
                File f = new File(c);
                if (f.exists()) {
                    return f;
                }
            }
        }
        System.err.println("ERROR: bok_terms.pdf not found in any temp directory.");
        System.err.println("Searched: " + Arrays.toString(candidates));
        System.exit(1);
        return null;
    }
}
