package io.github.sim2200.agenteval.evaluator;

import static org.assertj.core.api.Assertions.*;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonSchemaEvaluatorTest {

  private JsonSchemaEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new JsonSchemaEvaluator();
  }

  @Test
  void validDocumentPasses() {
    String json = "{\"tool\": \"hammer\"}";
    TestCase testCase = new TestCase("c1", "input", json, null);

    Map<String, Object> schema =
        Map.of(
            "type",
            "object",
            "required",
            new String[] {"tool"},
            "properties",
            Map.of("tool", Map.of("type", "string")));
    Check check = new Check("json_schema", Map.of("schema", schema), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Pass.class);
  }

  @Test
  void missingRequiredFieldFails() {
    String json = "{}";
    TestCase testCase = new TestCase("c1", "input", json, null);

    Map<String, Object> schema =
        Map.of(
            "type",
            "object",
            "required",
            new String[] {"tool"},
            "properties",
            Map.of("tool", Map.of("type", "string")));
    Check check = new Check("json_schema", Map.of("schema", schema), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
    assertThat(((Outcome.Fail) result).reason()).isNotEmpty();
  }

  @Test
  void invalidJsonOutputFails() {
    String invalidJson = "{not valid json}";
    TestCase testCase = new TestCase("c1", "input", invalidJson, null);

    Map<String, Object> schema = Map.of("type", "object");
    Check check = new Check("json_schema", Map.of("schema", schema), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
    assertThat(((Outcome.Fail) result).reason()).contains("JSON");
  }

  @Test
  void missingSchemaParamError() {
    String json = "{\"tool\": \"hammer\"}";
    TestCase testCase = new TestCase("c1", "input", json, null);
    Check check = new Check("json_schema", Map.of(), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }
}
