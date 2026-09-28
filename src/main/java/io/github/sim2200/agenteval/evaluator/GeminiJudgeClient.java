package io.github.sim2200.agenteval.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Calls the Gemini API (generateContent) with a short judging prompt and parses a JSON verdict.
 * Only constructed when {@code GEMINI_API_KEY} is set; the key is read from the environment and
 * never logged or stored.
 */
public class GeminiJudgeClient implements JudgeClient {

  private static final String ENDPOINT =
      "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String apiKey;
  private final String model;

  public GeminiJudgeClient(String apiKey, String model) {
    this.apiKey = apiKey;
    this.model = model;
  }

  @Override
  public String name() {
    return "gemini";
  }

  @Override
  public Verdict judge(String input, String output, String rubric) {
    String prompt =
        "You are grading an AI agent's output against a rubric.\n\nRubric: "
            + rubric
            + "\n\nUser input:\n"
            + input
            + "\n\nAgent output:\n"
            + output
            + "\n\nReply with JSON only: {\"satisfied\": true or false, \"reason\": \"one short sentence\"}";
    Map<String, Object> body =
        Map.of(
            "contents",
                java.util.List.of(Map.of("parts", java.util.List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("temperature", 0, "maxOutputTokens", 96));
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(ENDPOINT.formatted(model)))
              .timeout(Duration.ofSeconds(30))
              .header("Content-Type", "application/json")
              .header("x-goog-api-key", apiKey)
              .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
              .build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException("judge HTTP " + response.statusCode());
      }
      JsonNode root = mapper.readTree(response.body());
      String text = root.at("/candidates/0/content/parts/0/text").asText("");
      int open = text.indexOf('{');
      int close = text.lastIndexOf('}');
      if (open < 0 || close < open) {
        throw new IllegalStateException("judge returned no JSON");
      }
      JsonNode verdict = mapper.readTree(text.substring(open, close + 1));
      return new Verdict(
          verdict.path("satisfied").asBoolean(false), verdict.path("reason").asText(""));
    } catch (IOException e) {
      throw new IllegalStateException("judge call failed: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("judge call interrupted", e);
    }
  }
}
