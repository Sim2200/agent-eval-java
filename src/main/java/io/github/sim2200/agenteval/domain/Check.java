package io.github.sim2200.agenteval.domain;

import java.util.Map;

/**
 * One assertion on a case, as written in the suite YAML.
 *
 * @param type the evaluator type: contains, regex, json_schema, must_not_fire, llm_judge
 * @param params evaluator-specific parameters (e.g. {@code text}, {@code pattern}, {@code schema})
 * @param shouldPass ground truth for this case: {@code true} means the check is expected to hold;
 *     {@code false} marks a deliberate negative example where the check is expected to fire. This
 *     is what lets a run report precision and recall of fires per check type.
 */
public record Check(String type, Map<String, Object> params, boolean shouldPass) {

  public Check {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("check type is required");
    }
    params = params == null ? Map.of() : Map.copyOf(params);
  }

  public String param(String key) {
    Object v = params.get(key);
    return v == null ? null : v.toString();
  }
}
