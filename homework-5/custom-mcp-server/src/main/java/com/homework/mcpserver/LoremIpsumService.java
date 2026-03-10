package com.homework.mcpserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Service that owns all Lorem Ipsum business logic.
 *
 * <p>Content is loaded once at startup from the file resolved by
 * {@code app.lorem-ipsum.path} (default: {@code lorem-ipsum.md} in the working
 * directory).  If the file is not found there, the classpath is tried as a
 * fallback so the JAR remains self-contained for demo purposes.
 *
 * <h2>Roles</h2>
 * <ul>
 *   <li>{@link #read} — MCP <b>Tool</b>: an action the client explicitly invokes
 *       (analogous to calling a function).  It accepts an optional
 *       {@code wordCount} parameter and returns that many words of Lorem Ipsum
 *       text.</li>
 *   <li>{@link #getWords} — shared helper used by both the tool and the
 *       resource handler in {@link McpServerApplication}.</li>
 * </ul>
 */
@Service
public class LoremIpsumService {

    private static final Logger log = LoggerFactory.getLogger(LoremIpsumService.class);

    /** Default number of words returned when no {@code word_count} is supplied. */
    public static final int DEFAULT_WORD_COUNT = 30;

    @Value("${app.lorem-ipsum.path:lorem-ipsum.md}")
    private String loremIpsumPath;

    private String[] words;

    // -----------------------------------------------------------------------
    // Initialisation
    // -----------------------------------------------------------------------

    @PostConstruct
    public void loadContent() {
        Path fsPath = Paths.get(loremIpsumPath);
        try {
            String raw;
            if (Files.exists(fsPath)) {
                log.info("Loading lorem ipsum from file system: {}", fsPath.toAbsolutePath());
                raw = Files.readString(fsPath, StandardCharsets.UTF_8);
            } else {
                log.info("'{}' not found on file system, falling back to classpath", loremIpsumPath);
                var cp = getClass().getClassLoader().getResourceAsStream("lorem-ipsum.md");
                if (cp == null) {
                    throw new IOException("lorem-ipsum.md not found on classpath either");
                }
                raw = new String(cp.readAllBytes(), StandardCharsets.UTF_8);
            }
            words = tokenize(raw);
            log.info("Lorem ipsum loaded — {} words available", words.length);
        } catch (IOException e) {
            log.error("Failed to load lorem ipsum content: {}", e.getMessage());
            // Minimal fallback so the server stays alive
            words = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor"
                    .split("\\s+");
        }
    }

    // -----------------------------------------------------------------------
    // MCP Tool
    // -----------------------------------------------------------------------

    /**
     * MCP Tool: {@code read}.
     *
     * <p>Returns up to {@code wordCount} words of Lorem Ipsum text (default 30).
     * The client invokes this via the standard {@code tools/call} RPC:
     * <pre>
     *   { "name": "read", "arguments": { "wordCount": 50 } }
     * </pre>
     *
     * @param wordCount how many words to return; {@code null} uses the default (30).
     */
    @Tool(name = "read",
          description = "Read lorem ipsum content from lorem-ipsum.md. "
                  + "Returns wordCount words (default 30 when omitted). "
                  + "Example: wordCount=50 returns the first 50 words.")
    public String read(@Nullable Integer wordCount) {
        int count = (wordCount != null && wordCount > 0) ? wordCount : DEFAULT_WORD_COUNT;
        return getWords(count);
    }

    // -----------------------------------------------------------------------
    // Shared helper
    // -----------------------------------------------------------------------

    /**
     * Returns the first {@code count} words joined by a single space.
     * Never returns more words than are available in the source file.
     */
    public String getWords(int count) {
        if (words == null || words.length == 0) {
            return "";
        }
        int limit = Math.min(count, words.length);
        return String.join(" ", Arrays.copyOfRange(words, 0, limit));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Strips Markdown syntax characters and splits by whitespace so that word
     * counting operates on plain words rather than formatting tokens.
     */
    private static String[] tokenize(String markdown) {
        String plain = markdown
                .replaceAll("(?m)^#{1,6}\\s+", "")   // headings
                .replaceAll("[*_`~]", "")               // bold/italic/code
                .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1") // links
                .trim();
        return Arrays.stream(plain.split("\\s+"))
                .filter(w -> !w.isBlank())
                .toArray(String[]::new);
    }
}
