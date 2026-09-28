package io.github.sim2200.agenteval.evaluator;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvaluatorFactoryTest {

  private EvaluatorFactory factory;

  @BeforeEach
  void setUp() {
    List<Evaluator> evaluators =
        List.of(
            new ContainsEvaluator(),
            new RegexEvaluator(),
            new JsonSchemaEvaluator(),
            new MustNotFireEvaluator());
    factory = new EvaluatorFactory(evaluators);
  }

  @Test
  void buildsFromListOfEvaluators() {
    assertThat(factory.types()).hasSize(4);
  }

  @Test
  void forTypeReturnsTheRightOne() {
    Evaluator contains = factory.forType("contains");
    assertThat(contains).isInstanceOf(ContainsEvaluator.class);

    Evaluator regex = factory.forType("regex");
    assertThat(regex).isInstanceOf(RegexEvaluator.class);
  }

  @Test
  void unknownTypeThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> factory.forType("unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown")
        .hasMessageContaining("known");
  }

  @Test
  void typesReturnsSet() {
    assertThat(factory.types())
        .containsExactlyInAnyOrder("contains", "regex", "json_schema", "must_not_fire");
  }
}
