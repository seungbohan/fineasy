package com.fineasy.external.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.OpenAiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final WebClient webClient;
    private final OpenAiConfig config;
    private final ObjectMapper objectMapper;

    public OpenAiClient(@Qualifier("openAiWebClient") WebClient webClient,
                         OpenAiConfig config,
                         ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, 0, 0.7);
    }

    public String chat(String systemPrompt, String userPrompt, int maxTokens) {
        return chat(systemPrompt, userPrompt, maxTokens, 0.7);
    }

    public String chat(String systemPrompt, String userPrompt, int maxTokens, double temperature) {
        Map<String, Object> requestBody = new java.util.HashMap<>(Map.of(
                "model", config.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", temperature,
                "response_format", Map.of("type", "json_object")
        ));

        if (maxTokens > 0) {
            requestBody.put("max_tokens", maxTokens);
        }

        log.debug("Calling OpenAI API with model: {}, temp: {}, max_tokens: {}",
                config.getModel(), temperature, maxTokens > 0 ? maxTokens : "unlimited");

        return executeRequest(requestBody);
    }

    public String chatReasoning(String systemPrompt, String userPrompt, int maxTokens) {
        Map<String, Object> requestBody = new java.util.HashMap<>(Map.of(
                "model", config.getReasoningModel(),
                "messages", List.of(
                        Map.of("role", "developer", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object")
        ));

        if (maxTokens > 0) {
            requestBody.put("max_completion_tokens", maxTokens);
        }

        log.debug("Calling OpenAI Reasoning API with model: {}, max_completion_tokens: {}",
                config.getReasoningModel(), maxTokens > 0 ? maxTokens : "unlimited");

        return executeRequest(requestBody);
    }

    private String executeRequest(Map<String, Object> requestBody) {
        try {
            String responseBody = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(config.getTimeout());

            return extractContent(responseBody);
        } catch (WebClientResponseException e) {
            log.error("OpenAI API error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API call failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new RuntimeException("No choices in OpenAI response");
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", responseBody, e);
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }
}
