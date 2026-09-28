package io.github.sim2200.agenteval.api;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureProperties(
      org.springframework.test.context.DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void createSuite() throws Exception {
    String yaml = Files.readString(Path.of("suites/booking-agent.yaml"));
    ResponseEntity<String> response = restTemplate.postForEntity("/suites", yaml, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.has("id")).isTrue();
    assertThat(body.get("id").asText()).isEqualTo("booking-agent");
  }

  @Test
  void createRunAndRetrieve() throws Exception {
    // First create a suite
    String yaml = Files.readString(Path.of("suites/booking-agent.yaml"));
    ResponseEntity<String> suiteResponse =
        restTemplate.postForEntity("/suites", yaml, String.class);
    JsonNode suiteBody = objectMapper.readTree(suiteResponse.getBody());
    String suiteId = suiteBody.get("id").asText();

    // Create a run
    String runRequest =
        objectMapper.writeValueAsString(new ApiController.RunRequest(suiteId, 3, 16));
    ResponseEntity<String> runResponse =
        restTemplate.postForEntity("/runs", json(runRequest), String.class);

    assertThat(runResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    JsonNode runBody = objectMapper.readTree(runResponse.getBody());
    long runId = runBody.get("id").asLong();

    // Retrieve the run
    ResponseEntity<String> getResponse = restTemplate.getForEntity("/runs/" + runId, String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode getBody = objectMapper.readTree(getResponse.getBody());
    assertThat(getBody.get("id").asLong()).isEqualTo(runId);
  }

  @Test
  void createMultipleRuns() throws Exception {
    // Create a suite
    String yaml = Files.readString(Path.of("suites/booking-agent.yaml"));
    ResponseEntity<String> suiteResponse =
        restTemplate.postForEntity("/suites", yaml, String.class);
    JsonNode suiteBody = objectMapper.readTree(suiteResponse.getBody());
    String suiteId = suiteBody.get("id").asText();

    // Create two runs
    String runRequest =
        objectMapper.writeValueAsString(new ApiController.RunRequest(suiteId, 3, 16));
    ResponseEntity<String> run1Response =
        restTemplate.postForEntity("/runs", json(runRequest), String.class);
    JsonNode run1Body = objectMapper.readTree(run1Response.getBody());
    long run1Id = run1Body.get("id").asLong();

    ResponseEntity<String> run2Response =
        restTemplate.postForEntity("/runs", json(runRequest), String.class);
    JsonNode run2Body = objectMapper.readTree(run2Response.getBody());
    long run2Id = run2Body.get("id").asLong();

    assertThat(run1Id).isNotEqualTo(run2Id);
  }

  @Test
  void diffRuns() throws Exception {
    // Create a suite
    String yaml = Files.readString(Path.of("suites/booking-agent.yaml"));
    ResponseEntity<String> suiteResponse =
        restTemplate.postForEntity("/suites", yaml, String.class);
    JsonNode suiteBody = objectMapper.readTree(suiteResponse.getBody());
    String suiteId = suiteBody.get("id").asText();

    // Create two runs
    String runRequest =
        objectMapper.writeValueAsString(new ApiController.RunRequest(suiteId, 3, 16));
    ResponseEntity<String> run1Response =
        restTemplate.postForEntity("/runs", json(runRequest), String.class);
    JsonNode run1Body = objectMapper.readTree(run1Response.getBody());
    long run1Id = run1Body.get("id").asLong();

    ResponseEntity<String> run2Response =
        restTemplate.postForEntity("/runs", json(runRequest), String.class);
    JsonNode run2Body = objectMapper.readTree(run2Response.getBody());
    long run2Id = run2Body.get("id").asLong();

    // Diff them
    ResponseEntity<String> diffResponse =
        restTemplate.getForEntity("/runs/" + run1Id + "/diff/" + run2Id, String.class);

    assertThat(diffResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode diffBody = objectMapper.readTree(diffResponse.getBody());
    assertThat(diffBody.has("compared")).isTrue();
  }

  @Test
  void invalidYamlReturns400() {
    ResponseEntity<String> response =
        restTemplate.postForEntity("/suites", "not: [valid yaml: ", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void unknownRunReturns404() {
    ResponseEntity<String> response = restTemplate.getForEntity("/runs/99999", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createRunWithUnknownSuite() {
    String runRequest = "{\"suiteId\": \"unknown\", \"repeat\": 3, \"concurrency\": 16}";
    ResponseEntity<String> response =
        restTemplate.postForEntity("/runs", json(runRequest), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void openApiSpec() {
    ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private static HttpEntity<String> json(String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }
}
