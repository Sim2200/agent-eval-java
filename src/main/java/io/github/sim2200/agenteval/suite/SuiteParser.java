package io.github.sim2200.agenteval.suite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.TestCase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reads a suite from YAML:
 *
 * <pre>
 * id: booking-agent
 * name: Customer-service booking agent
 * cases:
 *   - id: b-001
 *     input: "I need to move my flight to Friday"
 *     output: "..."            # or transcript: "..."
 *     checks:
 *       - type: contains
 *         text: "Friday"
 *       - type: must_not_fire
 *         phrases: ["refund has been issued"]
 *         should_pass: false   # a negative example: this check is expected to fire
 * </pre>
 *
 * Every key on a check other than {@code type} and {@code should_pass} is passed to the evaluator
 * as a parameter, so new evaluators need no parser changes.
 */
@Component
public class SuiteParser {

  private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

  public Suite parse(String text) {
    Map<String, Object> root;
    try {
      root = yaml.readValue(text, new TypeReference<Map<String, Object>>() {});
    } catch (IOException e) {
      throw new SuiteFormatException("not valid YAML: " + e.getMessage());
    }
    if (root == null) {
      throw new SuiteFormatException("empty suite");
    }
    String id = str(root.get("id"));
    if (id == null) {
      throw new SuiteFormatException("suite 'id' is required");
    }
    String name = str(root.getOrDefault("name", id));
    Object rawCases = root.get("cases");
    if (!(rawCases instanceof List<?> caseList) || caseList.isEmpty()) {
      throw new SuiteFormatException("suite must have a non-empty 'cases' list");
    }
    List<TestCase> cases = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (Object item : caseList) {
      TestCase testCase = parseCase(item);
      if (!seen.add(testCase.id())) {
        throw new SuiteFormatException("duplicate case id '" + testCase.id() + "'");
      }
      cases.add(testCase);
    }
    return new Suite(id, name, cases);
  }

  @SuppressWarnings("unchecked")
  private TestCase parseCase(Object item) {
    if (!(item instanceof Map<?, ?> m)) {
      throw new SuiteFormatException("each case must be a mapping");
    }
    Map<String, Object> map = (Map<String, Object>) m;
    String id = str(map.get("id"));
    if (id == null) {
      throw new SuiteFormatException("case without 'id'");
    }
    String output = str(map.containsKey("output") ? map.get("output") : map.get("transcript"));
    Object rawChecks = map.get("checks");
    if (!(rawChecks instanceof List<?> checkList) || checkList.isEmpty()) {
      throw new SuiteFormatException("case '" + id + "' has no checks");
    }
    List<Check> checks = new ArrayList<>();
    for (Object c : checkList) {
      if (!(c instanceof Map<?, ?> cm)) {
        throw new SuiteFormatException("case '" + id + "': each check must be a mapping");
      }
      Map<String, Object> params = new java.util.LinkedHashMap<>((Map<String, Object>) cm);
      String type = str(params.remove("type"));
      Object shouldPass = params.remove("should_pass");
      if (type == null) {
        throw new SuiteFormatException("case '" + id + "': check without 'type'");
      }
      checks.add(
          new Check(
              type, params, shouldPass == null || Boolean.parseBoolean(shouldPass.toString())));
    }
    return new TestCase(id, str(map.get("input")), output, checks);
  }

  private static String str(Object o) {
    return o == null ? null : o.toString();
  }

  /** A parsed suite. */
  public record Suite(String id, String name, List<TestCase> cases) {}

  /** Thrown for malformed suites; mapped to HTTP 400 by the API. */
  public static class SuiteFormatException extends RuntimeException {
    public SuiteFormatException(String message) {
      super(message);
    }
  }
}
