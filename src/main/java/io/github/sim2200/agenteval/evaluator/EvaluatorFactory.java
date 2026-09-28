package io.github.sim2200.agenteval.evaluator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Looks up the {@link Evaluator} for a check type. Spring injects every {@code Evaluator} bean, so
 * adding a check type is one new class with {@code @Component} and nothing else changes.
 */
@Component
public class EvaluatorFactory {

  private final Map<String, Evaluator> byType;

  public EvaluatorFactory(List<Evaluator> evaluators) {
    this.byType =
        evaluators.stream()
            .collect(Collectors.toUnmodifiableMap(Evaluator::type, Function.identity()));
  }

  public Evaluator forType(String type) {
    Evaluator evaluator = byType.get(type);
    if (evaluator == null) {
      throw new IllegalArgumentException("unknown check type '" + type + "'; known: " + types());
    }
    return evaluator;
  }

  public Set<String> types() {
    return byType.keySet();
  }
}
