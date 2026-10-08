package com.dsatracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin wrapper around Google's Gemini free-tier API (generateContent). Every AI-powered feature
 * in this app degrades gracefully when {@code app.gemini.api-key} is unset -- a missing/failed
 * call returns {@link Optional#empty()} rather than throwing, so callers fall back to their own
 * non-AI behavior instead of breaking the feature entirely.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-flash-lite-latest}")
    private String model;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** One turn of a multi-turn conversation, in Gemini's own role vocabulary. */
    public record ChatTurn(String role, String text) {
        public static ChatTurn user(String text) {
            return new ChatTurn("user", text);
        }

        public static ChatTurn model(String text) {
            return new ChatTurn("model", text);
        }
    }

    /** Sends one prompt, no conversation history. Returns empty on any failure or when unconfigured. */
    public Optional<String> generate(String prompt) {
        if (!isConfigured()) return Optional.empty();
        Map<String, Object> body = Map.of(
                "contents", new Object[]{Map.of("parts", new Object[]{Map.of("text", prompt)})},
                "generationConfig", Map.of("temperature", 0.4, "maxOutputTokens", 512)
        );
        return send(body);
    }

    /**
     * Sends a full conversation (oldest turn first) plus an optional system instruction, so the
     * model can reference earlier turns -- used by the AI tutor chat and the mock interview,
     * unlike {@link #generate} which only ever sees one isolated prompt.
     */
    public Optional<String> generateChat(String systemInstruction, List<ChatTurn> turns) {
        if (!isConfigured()) return Optional.empty();
        Object[] contents = turns.stream()
                .map(t -> Map.of("role", t.role(), "parts", new Object[]{Map.of("text", t.text())}))
                .toArray();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.put("systemInstruction", Map.of("parts", new Object[]{Map.of("text", systemInstruction)}));
        }
        body.put("generationConfig", Map.of("temperature", 0.5, "maxOutputTokens", 512));
        return send(body);
    }

    private Optional<String> send(Map<String, Object> body) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent".formatted(model);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Gemini call failed with status {}: {}", response.statusCode(), truncate(response.body()));
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
            return text == null || text.isBlank() ? Optional.empty() : Optional.of(text.trim());
        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.toString());
            return Optional.empty();
        }
    }

    private static String truncate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
