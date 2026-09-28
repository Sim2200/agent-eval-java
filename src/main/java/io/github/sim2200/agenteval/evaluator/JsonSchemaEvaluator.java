package io.github.sim2200.agenteval.evaluator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Passes when the output parses as JSON and validates against {@code schema} (a JSON Schema object,
 * draft 2020-12). Used for tool-calling agents whose output must be machine-readable.
 */
@Component
public class JsonSchemaEvaluator implements Evaluator {

  private final ObjectMapper mapper = new ObjectMapper();
  private final JsonSchemaFactory factory =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  private final Map<Object, JsonSchema> cache = new ConcurrentHashMap<>();

  @Override
  public String type() {
    return "json_schema";
  }

  @Override
  public Outcome evaluate(TestCase testCase, Check check) {
    Object schema = check.params().get("schema");
    if (schema == null) {
      return new Outcome.Error("json_schema: 'schema' is required");
    }
    JsonNode document;
    try {
      document = mapper.readTree(testCase.output());
    } catch (JsonProcessingException e) {
      return new Outcome.Fail("output is not valid JSON: " + e.getOriginalMessage());
    }
    JsonSchema compiled;
    try {
      compiled = cache.computeIfAbsent(schema, s -> factory.getSchema(mapper.valueToTree(s)));
    } catch (RuntimeException e) {
      return new Outcome.Error("json_schema: invalid schema: " + e.getMessage());
    }
    Set<ValidationMessage> problems = compiled.validate(document);
    if (problems.isEmpty()) {
      return new Outcome.Pass();
    }
    return new Outcome.Fail(
        problems.stream()
            .map(ValidationMessage::getMessage)
            .sorted()
            .collect(Collectors.joining("; ")));
  }
}
